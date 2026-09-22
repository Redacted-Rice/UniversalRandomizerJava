package redactedrice.randomizer.lua.arguments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Helpers for two field tuple values stored as maps keyed by field name. */
public final class TupleEntry {
    private TupleEntry() {
    }

    public static Map<String, Object> of(String field0, Object value0, String field1, Object value1) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(field0, value0);
        entry.put(field1, value1);
        return entry;
    }

    public static Map<String, Object> of(List<TupleFieldDefinition> fields, Object value0,
            Object value1) {
        if (fields == null || fields.size() != 2) {
            throw new IllegalArgumentException("Tuple must have exactly two fields");
        }
        return of(fields.get(0).name(), value0, fields.get(1).name(), value1);
    }
}
