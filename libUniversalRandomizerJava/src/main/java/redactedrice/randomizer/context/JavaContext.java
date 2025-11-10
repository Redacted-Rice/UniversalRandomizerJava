package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// wrapper for java objects to pass to lua execution context
// lets lua scripts access and modify registered java objects
public class JavaContext {
    Map<String, Object> objects;
    EnumContext enumContext;
    Map<String, Object> config;

    public JavaContext() {
        this.objects = new HashMap<>();
        this.enumContext = new EnumContext();
        this.config = new HashMap<>();
    }

    public void register(String name, Object object) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        objects.put(name, object);
    }

    public <E extends Enum<E>> void registerEnum(Class<E> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumContext.registerEnum(enumClass);
    }

    public <E extends Enum<E>> void registerEnum(String name, Class<E> enumClass) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        enumContext.registerEnum(name, enumClass);
    }

    public void registerEnum(String name, String... values) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Enum values cannot be null or empty");
        }
        enumContext.registerEnum(name, Arrays.asList(values));
    }

    public EnumContext getEnumContext() {
        return enumContext;
    }

    public void mergeEnumContext(EnumContext source) {
        if (source != null) {
            enumContext.mergeFrom(source);
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

    public void setConfig(Map<String, Object> config) {
        if (config != null) {
            this.config = new HashMap<>(config);
        } else {
            this.config = new HashMap<>();
        }
    }

    public Map<String, Object> getConfig() {
        return new HashMap<>(config);
    }

    public LuaTable toLuaTable() {
        LuaTable table = new LuaTable();

        // Add regular objects with proper conversion
        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            LuaValue luaValue = javaToLuaValue(entry.getValue());
            table.set(entry.getKey(), luaValue);
        }

        // Add config as a table
        if (!config.isEmpty()) {
            LuaTable configTable = new LuaTable();
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                LuaValue luaValue = javaToLuaValue(entry.getValue());
                configTable.set(entry.getKey(), luaValue);
            }
            table.set("config", configTable);
        }

        // Add enums directly to root (not nested)
        if (!enumContext.getEnumNames().isEmpty()) {
            Map<String, LuaTable> luaEnums = enumContext.toLuaTables();
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

                LuaTable values = valuesTable.checktable();

                // Check if it's a simple array or has a values subtable
                LuaValue valuesSubtable = values.get("values");
                Map<String, Integer> valueMap = new LinkedHashMap<>();
                List<String> valueNames = new ArrayList<>();

                // First, extract the sequential array part (indices 1, 2, 3, etc.)
                // Iterate through array indices until we find nil or non-string
                // This ensures we only get the array part, not the hash part
                for (int i = 1;; i++) {
                    LuaValue value = values.get(i);
                    if (value.isnil() || (!value.isstring() && i == 1)) {
                        // Reached end of array part or first element is not a string
                        break;
                    }
                    if (value.isstring()) {
                        String enumValueName = value.tojstring();
                        // Skip "values" if it appears in the array part (shouldn't happen, but be
                        // safe)
                        if (!"values".equals(enumValueName)) {
                            valueNames.add(enumValueName);
                        }
                    } else {
                        // Found non-string at array index, stop here
                        break;
                    }
                }

                // If we have array elements, process them
                if (!valueNames.isEmpty()) {
                    // Then, if there's a values subtable, extract integer values from it
                    if (!valuesSubtable.isnil() && valuesSubtable.istable()) {
                        // Has values subtable - extract integer values
                        LuaTable valuesMapTable = valuesSubtable.checktable();
                        LuaValue key = LuaValue.NIL;
                        while (true) {
                            key = valuesMapTable.next(key).arg1();
                            if (key.isnil()) {
                                break;
                            }
                            if (key.isstring()) {
                                String enumValueName = key.tojstring();
                                LuaValue intValue = valuesMapTable.get(key);
                                if (intValue.isint() || intValue.isnumber()) {
                                    valueMap.put(enumValueName, intValue.toint());
                                }
                            }
                        }
                    } else {
                        // No values subtable - use 0-based indices as integer values
                        for (int i = 0; i < valueNames.size(); i++) {
                            valueMap.put(valueNames.get(i), i);
                        }
                    }
                } else {
                    // No array elements - check if it's a map-based enum (case 3)
                    // Iterate through all hash keys to find string keys with integer values
                    LuaValue key = LuaValue.NIL;
                    while (true) {
                        key = values.next(key).arg1();
                        if (key.isnil()) {
                            break;
                        }

                        // Skip the "values" key if present
                        if (key.isstring() && "values".equals(key.tojstring())) {
                            continue;
                        }

                        if (key.isstring()) {
                            String enumValueName = key.tojstring();
                            LuaValue intValue = values.get(key);
                            if (intValue.isint() || intValue.isnumber()) {
                                valueNames.add(enumValueName);
                                valueMap.put(enumValueName, intValue.toint());
                            }
                        }
                    }

                    // If we still don't have any values, throw an error
                    if (valueNames.isEmpty()) {
                        throw new RuntimeException(
                                "registerEnum: enum must have at least one value. "
                                        + "Provide either an array of strings, or a map of string keys to integer values.");
                    }
                }

                // Register the enum using the List<String> overload to preserve order
                // Use LinkedHashMap to preserve insertion order when converting back to Lua
                Map<String, Integer> orderedValueMap = new LinkedHashMap<>(valueMap);
                enumContext.registerEnum(enumName, valueNames, orderedValueMap);

                // Return the enum table (convert back to Lua format)
                Map<String, LuaTable> luaEnums = enumContext.toLuaTables();
                LuaTable newEnumTable = luaEnums.get(enumName);

                // Also add it to the context table for immediate access
                table.set(enumName, newEnumTable);

                return newEnumTable != null ? newEnumTable : LuaValue.NIL;
            }
        });

        return table;
    }

    // This only runs when adding to context
    private LuaValue javaToLuaValue(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        } else if (value instanceof List) {
            // Convert List to Lua table (1-indexed)
            List<?> list = (List<?>) value;
            LuaTable luaTable = new LuaTable();
            for (int i = 0; i < list.size(); i++) {
                luaTable.set(i + 1, javaToLuaValue(list.get(i)));
            }
            return luaTable;
        } else if (value instanceof Map) {
            // Convert Map to Lua table
            Map<?, ?> map = (Map<?, ?>) value;
            LuaTable luaTable = new LuaTable();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                LuaValue key = javaToLuaValue(entry.getKey());
                LuaValue val = javaToLuaValue(entry.getValue());
                luaTable.set(key, val);
            }
            return luaTable;
        } else if (value instanceof Enum) {
            // Convert enum to string (using name()) for primary string-based handling
            return LuaValue.valueOf(((Enum<?>) value).name());
        } else if (isPrimitiveOrWrapper(value) || value instanceof String) {
            // Primitives and strings: use direct coercion
            return CoerceJavaToLua.coerce(value);
        } else {
            // Wrap Java objects in extensible Lua tables
            return wrapJavaObjectInLuaTable(value);
        }
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
                                userdataValue, userdata);
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
            LuaValue originalMethod, LuaValue userdata) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                // Try to find the Java method using reflection
                Method javaMethod =
                        findJavaMethod(javaObject.getClass(), methodName, args.narg() - 1);

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
                                Object enumValue = enumContext.stringToEnum(
                                        paramTypes[paramIndex].getSimpleName(), stringValue);
                                if (enumValue == null) {
                                    // Try with custom enum names registered in EnumContext
                                    for (String enumName : enumContext.getEnumNames()) {
                                        enumValue = enumContext.stringToEnum(enumName, stringValue);
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
                    // Convert return value if it's a Java object
                    return convertReturnValue(result);
                } else {
                    // Method not found via reflection, call original method as-is
                    LuaValue[] newArgs = new LuaValue[args.narg()];
                    newArgs[0] = self;
                    for (int i = 1; i < args.narg(); i++) {
                        newArgs[i] = args.arg(i + 1);
                    }
                    Varargs result = originalMethod.invoke(LuaValue.varargsOf(newArgs));
                    // convert return value if it's a Java object
                    return convertReturnValue(result);
                }
            }
        };
    }

    // Converts from java map or list to lua table
    private Varargs convertReturnValue(Varargs result) {
        if (result.narg() == 0) {
            return result;
        }

        LuaValue firstValue = result.arg1();

        // convert java maps and lists to lua tables
        if (firstValue.isuserdata()) {
            Object javaObject = firstValue.touserdata();
            if (javaObject instanceof List || javaObject instanceof Map) {
                return javaToLuaValue(javaObject);
            }
        }

        return result;
    }

    // Cache for method lookups to avoid repeated reflection
    private static final Map<String, Method> methodCache = new HashMap<>();

    private Method findJavaMethod(Class<?> clazz, String methodName, int paramCount) {
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
                + enumContext.getEnumNames().size() + " enums: " + enumContext.getEnumNames() + "}";
    }
}
