package redactedrice.randomizer.utils;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.JavaObjectWrapper;

import java.util.*;


public class LuaJavaConverter {

    // ----------- Java to Lua Conversion ------------------

    public static LuaValue javaToLua(Object value) {
        return javaToLua(value, null);
    }

    public static LuaValue javaToLua(Object value, JavaObjectWrapper wrapper) {
        if (value == null) {
            return LuaValue.NIL;
        } else if (value instanceof LuaValue) {
            return (LuaValue) value;
        } else if (value instanceof Enum) {
            // Convert enum to string (using name())
            return LuaValue.valueOf(((Enum<?>) value).name());
        } else if (isPrimitiveOrWrapper(value) || value instanceof String) {
            // Use LuaJ's built-in coercion for primitives and strings
            return CoerceJavaToLua.coerce(value);
        } else if (value instanceof List) {
            return listToLuaTable((List<?>) value, wrapper);
        } else if (value instanceof Map) {
            return mapToLuaTable((Map<?, ?>) value, wrapper);
        } else {
            // For complex objects, wrap them if wrapper is provided
            if (wrapper != null) {
                return wrapper.wrap(value);
            } else {
                // Otherwise use LuaJ's coercion to make the userdata
                return CoerceJavaToLua.coerce(value);
            }
        }
    }

    public static LuaTable listToLuaTable(List<?> list) {
        return listToLuaTable(list, null);
    }

    public static LuaTable listToLuaTable(List<?> list, JavaObjectWrapper wrapper) {
        LuaTable luaTable = new LuaTable();
        for (int i = 0; i < list.size(); i++) {
            // Lua arrays are 1-indexed
            luaTable.set(i + 1, javaToLua(list.get(i), wrapper));
        }
        return luaTable;
    }

    public static LuaTable mapToLuaTable(Map<?, ?> map) {
        return mapToLuaTable(map, null);
    }

    public static LuaTable mapToLuaTable(Map<?, ?> map, JavaObjectWrapper wrapper) {
        LuaTable luaTable = new LuaTable();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            LuaValue key = javaToLua(entry.getKey(), wrapper);
            LuaValue val = javaToLua(entry.getValue(), wrapper);
            luaTable.set(key, val);
        }
        return luaTable;
    }

    public static LuaTable enumDefinitionToLuaTable(String enumName, EnumDefinition enumDef) {
        if (enumDef == null) {
            return null;
        }

        LuaTable enumTable = new LuaTable();
        List<String> values = enumDef.getValues();
        Map<String, Integer> valueMap = enumDef.getValueMap();
        Class<? extends Enum<?>> enumClass = enumDef.getEnumClass();

        // Create sequential array of strings (1-indexed) for pool/randomize use
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            enumTable.set(i + 1, LuaValue.valueOf(value));
        }

        // Named aliases so scripts can use EnumName.ENUM_VAL style access.
        // Skip metadata keys so a value with that name does not get overwritten below.
        for (String value : values) {
            if (isEnumTableMetadataKey(value)) {
                continue;
            }
            enumTable.set(value, toNamedEnumLuaValue(enumClass, value));
        }

        // Create values subtable mapping name -> integer value
        if (valueMap != null && !valueMap.isEmpty()) {
            LuaTable valuesTable = new LuaTable();
            for (Map.Entry<String, Integer> valueEntry : valueMap.entrySet()) {
                valuesTable.set(valueEntry.getKey(), LuaValue.valueOf(valueEntry.getValue()));
            }
            enumTable.set("values", valuesTable);
        }

        // Add metadata
        enumTable.set("_name", LuaValue.valueOf(enumName));

        Map<String, String> valueDisplayNames = enumDef.getValueDisplayNames();
        if (valueDisplayNames != null && !valueDisplayNames.isEmpty()) {
            LuaTable displayNamesTable = new LuaTable();
            for (Map.Entry<String, String> entry : valueDisplayNames.entrySet()) {
                displayNamesTable.set(entry.getKey(), LuaValue.valueOf(entry.getValue()));
            }
            enumTable.set("displayNames", displayNamesTable);
        }

        // Make the table read-only (best effort in LuaJ)
        enumTable.setmetatable(createReadOnlyEnumMetatable());

        return enumTable;
    }

    private static boolean isEnumTableMetadataKey(String key) {
        return "values".equals(key) || "_name".equals(key) || "displayNames".equals(key);
    }

    private static LuaValue toNamedEnumLuaValue(Class<? extends Enum<?>> enumClass, String value) {
        if (enumClass != null) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Enum<?> constant = Enum.valueOf((Class) enumClass, value);
                return CoerceJavaToLua.coerce(constant);
            } catch (IllegalArgumentException ignored) {
                // Fall through to string for extended values that are Lua only
            }
        }
        return LuaValue.valueOf(value);
    }

    private static LuaTable createReadOnlyEnumMetatable() {
        LuaTable mt = new LuaTable();
        // Prevent modifications
        mt.set("__newindex", LuaValue.valueOf("Enums are read-only"));
        return mt;
    }

    // ----------- Lua to Java Conversion ------------------

    public static Object luaToJava(LuaValue value) {
        return luaToJava(value, false);
    }

    public static Object luaToJava(LuaValue value, boolean skipTables) {
        if (value.isnil()) {
            return null;
        } else if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isint()) {
            return value.toint();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            if (skipTables) {
                return value;
            }
            LuaTable table = value.checktable();
            if (isLuaArray(table)) {
                return luaTableToList(table);
            } else {
                return luaTableToMap(table);
            }
        }
        return value.toString();
    }

    private static boolean isLuaArray(LuaTable table) {
        int length = table.length();
        if (length == 0) {
            return false;
        }
        // lua arrays have sequential keys from 1 to n
        for (int i = 1; i <= length; i++) {
            if (table.get(i).isnil()) {
                return false;
            }
        }
        return true;
    }

    private static List<Object> luaTableToList(LuaTable table) {
        List<Object> list = new ArrayList<>();
        int length = table.length();
        for (int i = 1; i <= length; i++) {
            list.add(luaToJava(table.get(i)));
        }
        return list;
    }

    private static Map<String, Object> luaTableToMap(LuaTable table) {
        Map<String, Object> map = new LinkedHashMap<>();
        LuaValue[] keys = table.keys();
        for (LuaValue key : keys) {
            if (key.isstring()) {
                LuaValue value = table.get(key);
                map.put(key.tojstring(), luaToJava(value));
            }
        }
        return map;
    }

    // ----------- Lua Table Field Extraction Utilities (Lua to Java) ------------------

    public static String tryGetStringFromTable(LuaTable table, String fieldName,
            String defaultValue, String context) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return defaultValue;
        }
        if (!value.isstring()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be a string (got "
                    + value.typename() + ")");
            return null;
        }
        return value.tojstring();
    }

    public static Integer tryGetIntFromTable(LuaTable table, String fieldName, String context,
            Integer defaultValue) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return defaultValue;
        }
        if (!value.isnumber() || !value.isint()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be an integer (got "
                    + value.typename() + "). Defaulting to " + defaultValue);
            return defaultValue;
        }
        return value.toint();
    }

    public static Integer tryGetIntFromTable(LuaTable table, String fieldName, String context) {
        return tryGetIntFromTable(table, fieldName, context, null);
    }

    public static Boolean tryGetBooleanFromTable(LuaTable table, String fieldName, String context,
            Boolean defaultValue) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return defaultValue;
        }
        if (!value.isboolean()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be a boolean (got "
                    + value.typename() + "). Defaulting to " + defaultValue);
            return defaultValue;
        }
        return value.toboolean();
    }

    public static Boolean tryGetBooleanFromTable(LuaTable table, String fieldName, String context) {
        return tryGetBooleanFromTable(table, fieldName, context, null);
    }

    public static LuaFunction tryGetFunctionFromTable(LuaTable table, String fieldName,
            String context) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return null;
        }
        if (!value.isfunction()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be a function (got "
                    + value.typename() + ")");
            return null;
        }
        return value.checkfunction();
    }

    public static Set<String> tryGetStringSetFromTable(LuaTable table, String fieldName,
            String context) {
        Set<String> result = new LinkedHashSet<>(); // Preserve order
        LuaValue value = table.get(fieldName);

        if (value.isnil()) {
            return result;
        }

        if (!value.istable()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be a table (got "
                    + value.typename() + ")");
            return null;
        }

        LuaTable valueTable = value.checktable();
        LuaValue key = LuaValue.NIL;
        while (true) {
            key = valueTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }
            LuaValue tableValue = valueTable.get(key);
            if (tableValue.isstring()) {
                String str = tableValue.tojstring();
                if (str != null && !str.trim().isEmpty()) {
                    result.add(str.trim());
                }
            } else {
                IssueTracker.addError(context + " field '" + fieldName
                        + "' must contain strings (got " + tableValue.typename() + ")");
                return null;
            }
        }

        return result;
    }

    public static Map<String, String> tryGetStringMapFromTable(LuaTable table, String fieldName,
            String context) {
        LuaValue value = table.get(fieldName);

        if (value.isnil()) {
            return null;
        }

        if (!value.istable()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be a table (got "
                    + value.typename() + ")");
            return null;
        }

        LuaTable mapTable = value.checktable();
        Map<String, String> result = new HashMap<>();

        LuaValue key = LuaValue.NIL;
        while (true) {
            key = mapTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }
            LuaValue mapValue = mapTable.get(key);

            if (key.isstring() && mapValue.isstring()) {
                result.put(key.tojstring(), mapValue.tojstring());
            } else {
                IssueTracker.addError(context + " field '" + fieldName
                        + "' must contain string keys and string values (got " + key.typename()
                        + " and " + mapValue.typename() + ")");
                return null;
            }
        }

        return result;
    }

    // ----------- Helper Methods ------------------

    private static boolean isPrimitiveOrWrapper(Object value) {
        return value instanceof Boolean || value instanceof Byte || value instanceof Character
                || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double;
    }
}
