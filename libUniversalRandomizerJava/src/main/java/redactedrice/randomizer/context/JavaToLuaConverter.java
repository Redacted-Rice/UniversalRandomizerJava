package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.*;
import java.util.Map.Entry;

/**
 * Converts Java values to Lua values for use in Lua execution. Handles primitives, collections, and
 * provides specific conversions
 */
public class JavaToLuaConverter {
    public static LuaValue convert(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        } else if (value instanceof LuaValue) {
            return (LuaValue) value;
        } else if (value instanceof List) {
            return listToLuaTable((List<?>) value);
        } else if (value instanceof Map) {
            return mapToLuaTable((Map<?, ?>) value);
        } else {
            // Use LuaJ's built-in coercion for primitives, strings, and other types
            return CoerceJavaToLua.coerce(value);
        }
    }

    public static LuaTable listToLuaTable(List<?> list) {
        LuaTable luaTable = new LuaTable();
        for (int i = 0; i < list.size(); i++) {
            // Lua arrays are 1-indexed
            luaTable.set(i + 1, convert(list.get(i)));
        }
        return luaTable;
    }

    public static LuaTable mapToLuaTable(Map<?, ?> map) {
        LuaTable luaTable = new LuaTable();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            LuaValue key = convert(entry.getKey());
            LuaValue val = convert(entry.getValue());
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

        // Create sequential array of strings (1-indexed)
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            enumTable.set(i + 1, LuaValue.valueOf(value));
        }

        // Create values subtable mapping name -> integer value
        if (valueMap != null && !valueMap.isEmpty()) {
            LuaTable valuesTable = new LuaTable();
            for (Entry<String, Integer> valueEntry : valueMap.entrySet()) {
                valuesTable.set(valueEntry.getKey(), LuaValue.valueOf(valueEntry.getValue()));
            }
            enumTable.set("values", valuesTable);
        }

        // Add metadata
        enumTable.set("_name", LuaValue.valueOf(enumName));

        // Make the table read-only (best effort in LuaJ)
        enumTable.setmetatable(createReadOnlyEnumMetatable());

        return enumTable;
    }

    private static LuaTable createReadOnlyEnumMetatable() {
        LuaTable mt = new LuaTable();
        // Prevent modifications
        mt.set("__newindex", LuaValue.valueOf("Enums are read-only"));
        return mt;
    }
}
