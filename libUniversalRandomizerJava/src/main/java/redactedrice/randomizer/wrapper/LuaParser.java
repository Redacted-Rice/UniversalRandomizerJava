package redactedrice.randomizer.wrapper;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.logger.ErrorTracker;

import java.util.*;

public class LuaParser {

    public static String parseString(LuaTable table, String fieldName, String defaultValue,
            String context) {
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

    public static Integer parseInt(LuaTable table, String fieldName, String context) {
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

    public static LuaFunction parseFunction(LuaTable table, String fieldName, String context) {
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

    public static Set<String> parseStringSet(LuaTable table, String fieldName, String context) {
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

    public static Map<String, String> parseStringMap(LuaTable table, String fieldName,
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

    public static Object luaValueToJava(LuaValue value) {
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
            list.add(luaValueToJava(table.get(i)));
        }
        return list;
    }

    private static Map<String, Object> luaTableToMap(LuaTable table) {
        Map<String, Object> map = new HashMap<>();
        LuaValue[] keys = table.keys();
        for (LuaValue key : keys) {
            if (key.isstring()) {
                LuaValue value = table.get(key);
                map.put(key.tojstring(), luaValueToJava(value));
            }
        }
        return map;
    }
}
