package redactedrice.randomizer.scripttests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Shared field readers for Lua case tables. Hosts use these for extra keys like cards.
public final class ScriptTestValues {
    public enum AccessType {
        WHOLE,
        ITEM
    }

    private static final String[] KEYED_METADATA = {
            "accessType", "getter", "setter", "pre", "post"
    };

    private ScriptTestValues() {}

    public static final class ListFieldSpec {
        private final AccessType accessType;
        private final String getterMethod;
        private final String setterMethod;
        private final String itemGetterMethod;
        private final String itemSetterMethod;
        private final String countGetterMethod;
        private final String countSetterMethod;
        private final List<String> pre;
        private final List<String> post;
        private final List<Map<String, Object>> values;

        private ListFieldSpec(AccessType accessType, String getterMethod, String setterMethod,
                String itemGetterMethod, String itemSetterMethod, String countGetterMethod,
                String countSetterMethod, List<String> pre, List<String> post,
                List<Map<String, Object>> values) {
            this.accessType = accessType;
            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
            this.itemGetterMethod = itemGetterMethod;
            this.itemSetterMethod = itemSetterMethod;
            this.countGetterMethod = countGetterMethod;
            this.countSetterMethod = countSetterMethod;
            this.pre = pre;
            this.post = post;
            this.values = values;
        }

        public AccessType accessType() {
            return accessType;
        }

        public String getterMethod() {
            return getterMethod;
        }

        public String setterMethod() {
            return setterMethod;
        }

        public String itemGetterMethod() {
            return itemGetterMethod;
        }

        public String itemSetterMethod() {
            return itemSetterMethod;
        }

        public String countGetterMethod() {
            return countGetterMethod;
        }

        public String countSetterMethod() {
            return countSetterMethod;
        }

        public List<String> pre() {
            return pre;
        }

        public List<String> post() {
            return post;
        }

        public List<Map<String, Object>> values() {
            return values;
        }
    }

    public static final class KeyedMapSpec {
        private final AccessType accessType;
        private final String getterMethod;
        private final String setterMethod;
        private final List<String> pre;
        private final List<String> post;
        private final Map<String, Object> entries;

        private KeyedMapSpec(AccessType accessType, String getterMethod, String setterMethod,
                List<String> pre, List<String> post, Map<String, Object> entries) {
            this.accessType = accessType;
            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
            this.pre = pre;
            this.post = post;
            this.entries = entries;
        }

        public AccessType accessType() {
            return accessType;
        }

        public String getterMethod() {
            return getterMethod;
        }

        public String setterMethod() {
            return setterMethod;
        }

        public List<String> pre() {
            return pre;
        }

        public List<String> post() {
            return post;
        }

        public Map<String, Object> entries() {
            return entries;
        }
    }

    public static boolean isListFieldSpec(Object value) {
        return value instanceof Map<?, ?> map && map.containsKey("values");
    }

    // List specs use "values". Keyed maps need explicit accessor metadata so pre/post hooks
    // on nested object tables are not mistaken for keyed map specs.
    public static boolean isKeyedMapSpec(Map<?, ?> map) {
        if (map == null || map.isEmpty() || map.containsKey("values")) {
            return false;
        }
        return map.containsKey("setter") || map.containsKey("getter")
                || map.containsKey("accessType");
    }

    @SuppressWarnings("unchecked")
    public static ListFieldSpec parseListFieldSpec(Object value, String field) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(field + " list must be a table with values");
        }
        Map<String, Object> spec = (Map<String, Object>) map;
        AccessType accessType = parseAccessType(spec.get("accessType"), field + ".accessType");

        String itemName = singularName(field);
        String itemGetterMethod = methodFromName("get", itemName);
        String itemSetterMethod = methodFromName("set", itemName);

        String getterMethod = optionalString(spec, "getter");
        String setterMethod = optionalString(spec, "setter");
        String countGetterMethod = null;
        String countSetterMethod = null;

        if (accessType == AccessType.WHOLE) {
            if (getterMethod == null) {
                getterMethod = methodFromName("get", field);
            }
            if (setterMethod == null) {
                setterMethod = methodFromName("set", field);
            }
        } else {
            if (getterMethod == null) {
                getterMethod = itemGetterMethod;
            }
            if (setterMethod == null) {
                setterMethod = itemSetterMethod;
            }
            countGetterMethod = optionalString(spec, "countGetter");
            countSetterMethod = optionalString(spec, "countSetter");
            if (countGetterMethod == null) {
                countGetterMethod = methodFromName("getNum", field);
            }
            if (countSetterMethod == null) {
                countSetterMethod = methodFromName("setNum", field);
            }
        }

        if (getterMethod == null) {
            throw new IllegalArgumentException(field + " list requires getter");
        }

        List<String> pre = optionalHookNames(spec.get("pre"), field + ".pre");
        List<String> post = optionalHookNames(spec.get("post"), field + ".post");
        List<Map<String, Object>> values = optionalListOfMaps(spec.get("values"), field + ".values");
        return new ListFieldSpec(accessType, getterMethod, setterMethod, itemGetterMethod,
                itemSetterMethod, countGetterMethod, countSetterMethod, pre, post, values);
    }

    public static KeyedMapSpec parseKeyedMapSpec(Map<String, Object> values, String field) {
        AccessType accessType = parseAccessType(values.get("accessType"), field + ".accessType");

        String getterMethod = optionalString(values, "getter");
        String setterMethod = optionalString(values, "setter");
        if (accessType == AccessType.WHOLE) {
            if (setterMethod == null) {
                setterMethod = methodFromName("set", field);
            }
            if (getterMethod == null) {
                getterMethod = methodFromName("get", field);
            }
        } else {
            String itemName = singularName(field);
            if (setterMethod == null) {
                setterMethod = methodFromName("set", itemName);
            }
            if (getterMethod == null) {
                getterMethod = methodFromName("get", itemName);
            }
        }
        if (setterMethod == null) {
            throw new IllegalArgumentException(field + " keyed map requires setter");
        }

        List<String> pre = optionalHookNames(values.get("pre"), field + ".pre");
        List<String> post = optionalHookNames(values.get("post"), field + ".post");
        Map<String, Object> entries = withoutKeys(values, KEYED_METADATA);
        return new KeyedMapSpec(accessType, getterMethod, setterMethod, pre, post, entries);
    }

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
        if (isListFieldSpec(value)) {
            value = ((Map<String, Object>) value).get("values");
            field = field + ".values";
        }
        if (value == null) {
            return List.of();
        }
        if (value instanceof Map<?, ?> emptyValues && emptyValues.isEmpty()) {
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

    // Null if the key is missing. Use this when absent vs present matters.
    public static List<Map<String, Object>> optionalTables(Map<String, Object> table, String field) {
        if (table == null || !table.containsKey(field) || table.get(field) == null) {
            return null;
        }
        return listOfMaps(table.get(field), field);
    }

    public static Map<String, Object> withoutKey(Map<String, Object> table, String key) {
        return withoutKeys(table, key);
    }

    public static Map<String, Object> withoutKeys(Map<String, Object> table, String... keys) {
        if (table == null || keys == null || keys.length == 0) {
            return table;
        }
        Map<String, Object> copy = new LinkedHashMap<>(table);
        for (String key : keys) {
            if (key != null) {
                copy.remove(key);
            }
        }
        return copy;
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

    private static AccessType parseAccessType(Object value, String field) {
        if (value == null) {
            return AccessType.WHOLE;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Field '" + field + "' must be a string");
        }
        if ("whole".equals(text)) {
            return AccessType.WHOLE;
        }
        if ("item".equals(text)) {
            return AccessType.ITEM;
        }
        throw new IllegalArgumentException(
                "Field '" + field + "' must be 'whole' or 'item'");
    }

    // Naive plural trim only. Irregular names like "class" need explicit getter/setter in the spec.
    private static String singularName(String fieldName) {
        if (fieldName == null || fieldName.length() <= 1) {
            return fieldName;
        }
        if (fieldName.endsWith("s")) {
            return fieldName.substring(0, fieldName.length() - 1);
        }
        return fieldName;
    }

    private static String methodFromName(String prefix, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return prefix + cap(name);
    }

    private static String cap(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static List<String> optionalHookNames(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String name) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Field '" + field + "' must be a non blank string");
            }
            return List.of(name);
        }
        if (value instanceof List<?> list) {
            List<String> names = new ArrayList<>();
            for (Object entry : list) {
                if (!(entry instanceof String name) || name.isBlank()) {
                    throw new IllegalArgumentException(
                            field + " entries must be non blank strings");
                }
                names.add(name);
            }
            return names;
        }
        throw new IllegalArgumentException(
                field + " must be a string or an array of strings");
    }
}
