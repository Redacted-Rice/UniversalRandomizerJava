package redactedrice.randomizer.scripttests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Shared field readers for Lua case tables. Hosts use these for extra keys like cards.
public final class ScriptTestValues {
    private ScriptTestValues() {}

    public static String optionalString(Map<String, Object> table, String field) {
        Object value = table.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Field '" + field + "' must be a string");
        }
        return text.isBlank() ? null : text;
    }

    public static String requiredString(Map<String, Object> table, String field) {
        Object value = table.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing string field '" + field + "'");
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> optionalMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected a table");
        }
        return (Map<String, Object>) map;
    }

    public static List<Map<String, Object>> listOfMaps(Object value, String field) {
        List<Map<String, Object>> entries = optionalListOfMaps(value, field);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException(
                    field + " must be an array of tables with at least one entry");
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> optionalListOfMaps(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(field + " must be an array of tables");
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException(field + " entries must be tables");
            }
            entries.add((Map<String, Object>) map);
        }
        return entries;
    }

    public static int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected a number but got " + value);
    }
}
