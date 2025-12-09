package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import redactedrice.randomizer.utils.LuaJavaConverter;
import java.lang.reflect.Method;
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

    public JavaContext() {
        this.objects = new HashMap<>();
        this.config = new HashMap<>();
        this.enumRegistry = new EnumRegistry();
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
                luaValue = wrapJavaObjectInLuaTable(value);
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

    private LuaValue wrapJavaObjectInLuaTable(Object javaObject) {
        LuaValue userdata = CoerceJavaToLua.coerce(javaObject);

        // Create an extensible wrapper table
        LuaTable wrapper = new LuaTable();

        Map<String, Method> methodCache = new HashMap<>();

        // Create metatable for forwarding to Java object
        LuaTable metatable = new LuaTable();

        // __index: Try wrapper first, then userdata
        metatable.set(LuaValue.INDEX, new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                // First check the wrapper table itself
                LuaValue wrapperValue = wrapper.rawget(key);
                if (!wrapperValue.isnil()) {
                    return wrapperValue;
                }

                // Then try the userdata (Java object)
                try {
                    LuaValue userdataValue = userdata.get(key);

                    // If it's a function, wrap it to convert string enum parameters
                    if (userdataValue.isfunction()) {
                        return wrapMethodForEnumConversion(javaObject, key.toString(),
                                userdataValue, userdata, methodCache);
                    }

                    return userdataValue;
                } catch (Exception e) {
                    return LuaValue.NIL;
                }
            }
        });

        // __newindex: Always store in wrapper table for extensibility
        metatable.set(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                // Try to set on userdata first (for actual Java fields)
                try {
                    userdata.set(key, value);
                } catch (Exception e) {
                    // If that fails, store in wrapper (for dynamic Lua fields)
                    wrapper.rawset(key, value);
                }
                return LuaValue.NIL;
            }
        });

        // Store reference to underlying userdata for debugging
        wrapper.rawset("__userdata", userdata);

        // Apply metatable
        wrapper.setmetatable(metatable);

        return wrapper;
    }

    private LuaValue wrapMethodForEnumConversion(Object javaObject, String methodName,
            LuaValue originalMethod, LuaValue userdata, Map<String, Method> methodCache) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                // Try to find the Java method using reflection
                Method javaMethod = findJavaMethod(javaObject.getClass(), methodName,
                        args.narg() - 1, methodCache);

                // Always use userdata as 'self' (first argument)
                LuaValue self = userdata;
                if (args.narg() > 0 && args.arg(1).istable()) {
                    // Check if it's our wrapper by looking for __userdata field
                    LuaValue wrapperUserdata = args.arg(1).get("__userdata");
                    if (!wrapperUserdata.isnil() && wrapperUserdata == userdata) {
                        self = userdata;
                    } else {
                        self = args.arg(1);
                    }
                }

                if (javaMethod != null) {
                    // Convert arguements, converting strings to enums when appropriate
                    LuaValue[] newArgs = new LuaValue[args.narg()];
                    Class<?>[] paramTypes = javaMethod.getParameterTypes();

                    // First arg is 'self'
                    newArgs[0] = self;

                    // Convert remaining args
                    for (int i = 1; i < args.narg(); i++) {
                        LuaValue arg = args.arg(i + 1);
                        int paramIndex = i - 1; // Parameter index (0-based, excluding 'self')

                        if (paramIndex < paramTypes.length && paramTypes[paramIndex].isEnum()) {
                            // This parameter is an enum - try to convert string to enum
                            if (arg.isstring()) {
                                String stringValue = arg.tojstring();
                                Object enumValue = enumRegistry.stringToEnum(
                                        paramTypes[paramIndex].getSimpleName(), stringValue);
                                if (enumValue == null) {
                                    // Try with custom enum names registered in EnumRegistry
                                    for (String enumName : enumRegistry.getEnumNames()) {
                                        enumValue =
                                                enumRegistry.stringToEnum(enumName, stringValue);
                                        if (enumValue != null
                                                && enumValue.getClass() == paramTypes[paramIndex]) {
                                            break;
                                        }
                                    }
                                }
                                if (enumValue != null) {
                                    newArgs[i] = CoerceJavaToLua.coerce(enumValue);
                                } else {
                                    newArgs[i] = arg; // Keep original if conversion fails
                                }
                            } else {
                                newArgs[i] = arg;
                            }
                        } else {
                            newArgs[i] = arg;
                        }
                    }

                    Varargs result = originalMethod.invoke(LuaValue.varargsOf(newArgs));
                    return convertReturnValue(result);
                } else {
                    // Method not found via reflection, call original method as-is
                    LuaValue[] newArgs = new LuaValue[args.narg()];
                    newArgs[0] = self;
                    for (int i = 1; i < args.narg(); i++) {
                        newArgs[i] = args.arg(i + 1);
                    }
                    Varargs result = originalMethod.invoke(LuaValue.varargsOf(newArgs));
                    return convertReturnValue(result);
                }
            }

            private Varargs convertReturnValue(Varargs result) {
                // Convert return value if it's a Java collection
                LuaValue firstValue = result.narg() > 0 ? result.arg1() : LuaValue.NIL;
                if (firstValue.isuserdata()) {
                    Object javaObject = firstValue.touserdata();
                    if (javaObject instanceof List || javaObject instanceof Map) {
                        return LuaJavaConverter.javaToLua(javaObject);
                    }
                }
                return result;
            }
        };
    }

    private Method findJavaMethod(Class<?> clazz, String methodName, int paramCount,
            Map<String, Method> methodCache) {
        String cacheKey = clazz.getName() + "#" + methodName + "#" + paramCount;
        Method cached = methodCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Search for method
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                methodCache.put(cacheKey, method);
                return method;
            }
        }

        // Cache null result to avoid repeated searches
        methodCache.put(cacheKey, null);
        return null;
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
