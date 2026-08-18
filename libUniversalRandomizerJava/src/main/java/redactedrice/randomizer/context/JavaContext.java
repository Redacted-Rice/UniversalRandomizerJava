package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import redactedrice.randomizer.utils.LuaJavaConverter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// wrapper for java objects to pass to lua execution context
// lets lua scripts access and modify registered java objects
public class JavaContext {
    public static final String CHANGE_DETECTION_ACTIVE = "changeDetectionActive";

    Map<String, Object> objects;
    Map<String, Object> config;
    EnumRegistry enumRegistry;
    JavaObjectWrapper objectWrapper;
    String executionModuleName;

    public JavaContext() {
        this.objects = new HashMap<>();
        this.config = new HashMap<>();
        this.enumRegistry = new EnumRegistry();
        this.objectWrapper = new JavaObjectWrapper(enumRegistry);
    }

    public void register(String name, Object object) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        objects.put(name, object);
    }

    public void setConfig(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Config key cannot be null or empty");
        }
        config.put(key, value);
    }

    public Object getConfig(String key) {
        return config.get(key);
    }

    public void setExecutionModuleName(String executionModuleName) {
        this.executionModuleName = executionModuleName;
    }

    public void clearExecutionModuleName() {
        this.executionModuleName = null;
    }

    public String getExecutionModuleName() {
        return executionModuleName;
    }

    public <E extends Enum<E>> void registerEnum(Class<E> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumRegistry.registerEnum(enumClass);
    }

    public <E extends Enum<E>> void registerEnum(Class<E> enumClass,
            Map<String, String> valueDisplayNames) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumRegistry.registerEnum(enumClass.getSimpleName(), enumClass, valueDisplayNames);
    }

    public <E extends Enum<E>> void registerEnum(String name, Class<E> enumClass) {
        registerEnum(name, enumClass, null);
    }

    public <E extends Enum<E>> void registerEnum(String name, Class<E> enumClass,
            Map<String, String> valueDisplayNames) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumRegistry.registerEnum(name, enumClass, valueDisplayNames);
    }

    public void registerEnum(String name, String... values) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Enum values cannot be null or empty");
        }
        enumRegistry.registerEnum(name, Arrays.asList(values));
    }

    public EnumRegistry getEnumRegistry() {
        return enumRegistry;
    }

    public void mergeEnumRegistry(EnumRegistry source) {
        if (source != null) {
            enumRegistry.mergeFrom(source);
        }
    }

    // Types from module provides. Same map live Lua assignment uses for dynamic fields.
    public void mergeDynamicFieldTypes(Map<String, String> types) {
        objectWrapper.mergeDynamicFieldTypes(types);
    }

    public void registerDynamicField(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dynamic field name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Dynamic field type cannot be null or empty");
        }
        objectWrapper.mergeDynamicFieldTypes(Map.of(name, type));
    }

    public Object get(String name) {
        return objects.get(name);
    }

    public Object remove(String name) {
        return objects.remove(name);
    }

    public boolean contains(String name) {
        return objects.containsKey(name);
    }

    public void clear() {
        objects.clear();
        objectWrapper.clearCache();
    }

    /** Clears cached object wrappers without removing registered objects */
    public void clearWrapperCache() {
        objectWrapper.clearCache();
    }

    // Same wrapper cache Lua uses, so dynamic Lua fields stick around
    public LuaValue wrap(Object javaObject) {
        return objectWrapper.wrap(javaObject);
    }

    public String[] getRegisteredNames() {
        return objects.keySet().toArray(new String[0]);
    }

    public LuaTable toLuaTable() {
        LuaTable table = new LuaTable();

        // Add regular objects with proper conversion
        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            Object value = entry.getValue();
            LuaValue luaValue;

            // Wrap complex objects for method access, including objects inside lists/maps
            if (value != null && !isPrimitiveOrWrapper(value) && !(value instanceof String)
                    && !(value instanceof Enum)) {
                // Pass wrapper to converter so it can wrap nested objects
                luaValue = LuaJavaConverter.javaToLua(value, objectWrapper);
            } else {
                luaValue = LuaJavaConverter.javaToLua(value);
            }
            table.set(entry.getKey(), luaValue);
        }

        // Add config as a table
        if (!config.isEmpty()) {
            LuaTable configTable = new LuaTable();
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                configTable.set(entry.getKey(), LuaJavaConverter.javaToLua(entry.getValue()));
            }
            table.set("config", configTable);
        }

        if (executionModuleName != null) {
            table.set("executionModule", LuaValue.valueOf(executionModuleName));
        }

        // Add enums directly to root (not nested)
        if (!enumRegistry.getEnumNames().isEmpty()) {
            Map<String, LuaTable> luaEnums = enumRegistry.toLuaTables();
            for (Map.Entry<String, LuaTable> enumEntry : luaEnums.entrySet()) {
                table.set(enumEntry.getKey(), enumEntry.getValue());
            }
        }

        // Add registerEnum function for dynamic enum registration from Lua
        table.set("registerEnum", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue name, LuaValue valuesTable) {
                String enumName = name.checkjstring();

                if (!valuesTable.istable()) {
                    throw new RuntimeException("registerEnum: values must be a table");
                }

                // Parse & register enum
                ParsedEnumData parsedEnum =
                        LuaEnumTableParser.parseEnumTable(enumName, valuesTable.checktable());
                Map<String, Integer> orderedValueMap =
                        new LinkedHashMap<>(parsedEnum.getValueMap());
                
                enumRegistry.registerEnum(enumName, parsedEnum.getValueNames(), orderedValueMap,
                        parsedEnum.getValueDisplayNames());

                // Return the enum table (convert back to Lua format)
                Map<String, LuaTable> luaEnums = enumRegistry.toLuaTables();
                LuaTable newEnumTable = luaEnums.get(enumName);

                // Also add it to the context table for immediate access
                table.set(enumName, newEnumTable);

                return newEnumTable != null ? newEnumTable : LuaValue.NIL;
            }
        });

        // Add extendEnum function for extending existing enums from Lua
        table.set("extendEnum", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue name, LuaValue valuesTable) {
                String enumName = name.checkjstring();

                if (!valuesTable.istable()) {
                    throw new RuntimeException("extendEnum: values must be a table");
                }

                // Parse the values to add as an enum
                ParsedEnumData parsedEnum =
                        LuaEnumTableParser.parseEnumTable(enumName, valuesTable.checktable());
                Map<String, Integer> orderedValueMap =
                        new LinkedHashMap<>(parsedEnum.getValueMap());

                // Extend the enum with the parsed values
                EnumDefinition extended = enumRegistry.extendEnum(enumName,
                        parsedEnum.getValueNames(), orderedValueMap,
                        parsedEnum.getValueDisplayNames());

                // Convert null if extend failed because target enum didn't exist
                if (extended == null) {
                    return LuaValue.NIL;
                }

                // Return the updated enum
                Map<String, LuaTable> luaEnums = enumRegistry.toLuaTables();
                LuaTable extendedEnumTable = luaEnums.get(enumName);

                // Also update the context
                table.set(enumName, extendedEnumTable);

                return extendedEnumTable != null ? extendedEnumTable : LuaValue.NIL;
            }
        });

        return table;
    }

    private boolean isPrimitiveOrWrapper(Object value) {
        return value instanceof Boolean || value instanceof Byte || value instanceof Character
                || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double;
    }

    public int size() {
        return objects.size();
    }

    @Override
    public String toString() {
        return "JavaContext{" + objects.size() + " objects: " + objects.keySet() + ", "
                + enumRegistry.getEnumNames().size() + " enums: " + enumRegistry.getEnumNames()
                + "}";
    }
}
