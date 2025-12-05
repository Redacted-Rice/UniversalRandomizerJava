package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.utils.ErrorTracker;

import java.util.*;

public class LuaToJavaConverter {

    // ---------------- Generic/Auto conversion & helpers ----------------

    public static Object convert(LuaValue value) {
        return convert(value, false);
    }

    public static Object convert(LuaValue value, boolean skipTables) {
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
            list.add(convert(table.get(i)));
        }
        return list;
    }

    private static Map<String, Object> luaTableToMap(LuaTable table) {
        Map<String, Object> map = new HashMap<>();
        LuaValue[] keys = table.keys();
        for (LuaValue key : keys) {
            if (key.isstring()) {
                LuaValue value = table.get(key);
                map.put(key.tojstring(), convert(value));
            }
        }
        return map;
    }

    // ---------------- Specific primitives conversion ----------------

    public static String convertToString(Object value) {
        return value.toString();
    }

    public static Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to integer");
            }
        } else if (value instanceof LuaValue) {
            return ((LuaValue) value).toint();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getSimpleName() + " to integer");
    }

    public static Double convertToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to double");
            }
        } else if (value instanceof LuaValue) {
            return ((LuaValue) value).todouble();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getSimpleName() + " to double");
    }

    public static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            if (str.equals("true") || str.equals("yes") || str.equals("1")) {
                return true;
            } else if (str.equals("false") || str.equals("no") || str.equals("0")) {
                return false;
            }
            throw new IllegalArgumentException("Cannot convert '" + value + "' to boolean");
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof LuaValue) {
            return ((LuaValue) value).toboolean();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getSimpleName() + " to boolean");
    }

    // ---------------- Extract values from tables ----------------

    public static String tryGetStringFromTable(LuaTable table, String fieldName,
            String defaultValue, String context) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return defaultValue;
        }
        if (!value.isstring()) {
            ErrorTracker.addError(context + " field '" + fieldName + "' must be a string (got "
                    + value.typename() + ")");
            return null;
        }
        return value.tojstring();
    }

    public static Integer tryGetIntFromTable(LuaTable table, String fieldName, String context) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return null;
        }
        if (!value.isnumber() || !value.isint()) {
            ErrorTracker.addError(context + " field '" + fieldName + "' must be an integer (got "
                    + value.typename() + ")");
            return null;
        }
        return value.toint();
    }

    public static LuaFunction tryGetFunctionFromTable(LuaTable table, String fieldName,
            String context) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return null;
        }
        if (!value.isfunction()) {
            ErrorTracker.addError(context + " field '" + fieldName + "' must be a function (got "
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
            ErrorTracker.addError(context + " field '" + fieldName + "' must be a table (got "
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
                    result.add(str.toLowerCase());
                }
            } else {
                ErrorTracker.addError(context + " field '" + fieldName
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
            ErrorTracker.addError(context + " field '" + fieldName + "' must be a table (got "
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
                ErrorTracker.addError(context + " field '" + fieldName
                        + "' must contain string keys and string values (got " + key.typename()
                        + " and " + mapValue.typename() + ")");
                return null;
            }
        }

        return result;
    }
}
