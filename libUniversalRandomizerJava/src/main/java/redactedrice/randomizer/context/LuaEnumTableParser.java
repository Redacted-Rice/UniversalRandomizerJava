package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;


public class LuaEnumTableParser {

    public static ParsedEnumData parseEnumTable(String enumName, LuaTable valuesTable) {
        if (valuesTable == null) {
            throw new RuntimeException("registerEnum: values must be a table");
        }

        Map<String, String> valueDisplayNames = readStringMap(valuesTable, "displayNames");

        // Check if it's a simple array or has a values subtable
        LuaValue valuesSubtable = valuesTable.get("values");
        Map<String, Integer> valueMap = new LinkedHashMap<>();
        List<String> valueNames = new ArrayList<>();

        // First, extract the sequential array part (indices 1, 2, 3, etc.)
        extractArrayPart(valuesTable, valueNames);

        // If we have array elements, process them
        if (!valueNames.isEmpty()) {
            processArrayBasedEnum(valuesSubtable, valueMap, valueNames);
        } else {
            // No array elements - check if it's a map-based enum (case 3)
            extractMapBasedEnum(valuesTable, valueNames, valueMap);
        }

        // If we still don't have any values, throw an error
        if (valueNames.isEmpty()) {
            throw new RuntimeException("registerEnum: enum must have at least one value. "
                    + "Provide either an array of strings, or a map of string keys to integer values.");
        }

        return new ParsedEnumData(valueNames, valueMap, valueDisplayNames);
    }

    private static Map<String, String> readStringMap(LuaTable table, String fieldName) {
        LuaValue value = table.get(fieldName);
        if (value.isnil() || !value.istable()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        LuaTable mapTable = value.checktable();
        LuaValue key = LuaValue.NIL;
        while (true) {
            key = mapTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }
            if (key.isstring()) {
                LuaValue mapValue = mapTable.get(key);
                if (mapValue.isstring()) {
                    result.put(key.tojstring(), mapValue.tojstring());
                }
            }
        }
        return result;
    }

    private static boolean isMetadataKey(String key) {
        return "values".equals(key) || "displayNames".equals(key);
    }

    private static void extractArrayPart(LuaTable valuesTable, List<String> valueNames) {
        // Iterate through array indices until we find nil or non-string
        for (int i = 1;; i++) {
            LuaValue value = valuesTable.get(i);
            if (value.isnil() || (!value.isstring() && i == 1)) {
                // Reached end of array part or first element is not a string
                break;
            }
            if (value.isstring()) {
                String enumValueName = value.tojstring();
                if (!isMetadataKey(enumValueName)) {
                    valueNames.add(enumValueName);
                }
            } else {
                // Found non-string at array index, stop here
                break;
            }
        }
    }

    private static void processArrayBasedEnum(LuaValue valuesSubtable,
            Map<String, Integer> valueMap, List<String> valueNames) {
        if (!valuesSubtable.isnil() && valuesSubtable.istable()) {
            // Has values subtable - extract integer values from it
            extractIntegerValues(valuesSubtable.checktable(), valueMap);
        } else {
            // No values subtable - use 0-based indices as integer values
            for (int i = 0; i < valueNames.size(); i++) {
                valueMap.put(valueNames.get(i), i);
            }
        }
    }

    private static void extractIntegerValues(LuaTable valuesMapTable,
            Map<String, Integer> valueMap) {
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
    }

    private static void extractMapBasedEnum(LuaTable valuesTable, List<String> valueNames,
            Map<String, Integer> valueMap) {
        // Iterate through all hash keys to find string keys with integer values
        LuaValue key = LuaValue.NIL;
        while (true) {
            key = valuesTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }

            if (key.isstring() && isMetadataKey(key.tojstring())) {
                continue;
            }

            if (key.isstring()) {
                String enumValueName = key.tojstring();
                LuaValue intValue = valuesTable.get(key);
                if (intValue.isint() || intValue.isnumber()) {
                    valueNames.add(enumValueName);
                    valueMap.put(enumValueName, intValue.toint());
                }
            }
        }
    }
}
