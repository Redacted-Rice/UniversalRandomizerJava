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
    Map<String, Object> objects;
    Map<String, Object> config;
    EnumRegistry enumRegistry;
    JavaObjectWrapper objectWrapper;

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

    public <E extends Enum<E>> void registerEnum(Class<E> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumRegistry.registerEnum(enumClass);
    }

    public <E extends Enum<E>> void registerEnum(String name, Class<E> enumClass) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumRegistry.registerEnum(name, enumClass);
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

            // Wrap complex objects for method access
            if (value != null && !isPrimitiveOrWrapper(value) && !(value instanceof String)
                    && !(value instanceof List) && !(value instanceof Map)
                    && !(value instanceof Enum)) {
                luaValue = objectWrapper.wrap(value);
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
                
                enumRegistry.registerEnum(enumName, parsedEnum.getValueNames(), orderedValueMap);

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
                        parsedEnum.getValueNames(), orderedValueMap);

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
