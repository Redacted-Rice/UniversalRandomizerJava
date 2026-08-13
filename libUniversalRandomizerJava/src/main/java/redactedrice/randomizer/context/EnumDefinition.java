package redactedrice.randomizer.context;

import java.util.*;

// holds an enum definition with its name and values
public class EnumDefinition {
    String name;
    List<String> values;
    Map<String, Integer> valueMap; // Maps enum name to integer value
    Class<? extends Enum<?>> enumClass;
    Map<String, String> valueDisplayNames; // canonical value -> display label

    public EnumDefinition(String name, List<String> values, Map<String, Integer> valueMap,
            Class<? extends Enum<?>> enumClass) {
        this(name, values, valueMap, enumClass, null);
    }

    public EnumDefinition(String name, List<String> values, Map<String, Integer> valueMap,
            Class<? extends Enum<?>> enumClass, Map<String, String> valueDisplayNames) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
        this.valueMap =
                valueMap != null ? Collections.unmodifiableMap(valueMap) : Collections.emptyMap();
        this.enumClass = enumClass;
        this.valueDisplayNames = valueDisplayNames != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(valueDisplayNames))
                : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public Map<String, Integer> getValueMap() {
        return valueMap;
    }

    public Integer getValue(String enumName) {
        return valueMap.get(enumName);
    }

    public Map<String, String> getValueDisplayNames() {
        return valueDisplayNames;
    }

    public String getValueDisplayName(String canonicalValue) {
        if (canonicalValue == null) {
            return null;
        }
        String display = valueDisplayNames.get(canonicalValue);
        return display != null && !display.isBlank() ? display : canonicalValue;
    }

    public boolean hasValue(String value) {
        return resolveCanonicalValue(value) != null;
    }

    // Exact matches win over case insensitive ones so a display label like "Water" is not
    // swallowed by another canonical named WATER.
    public String resolveCanonicalValue(String input) {
        if (input == null) {
            return null;
        }
        for (String canonical : values) {
            if (canonical.equals(input)) {
                return canonical;
            }
        }
        for (String canonical : values) {
            String display = valueDisplayNames.get(canonical);
            if (display != null && display.equals(input)) {
                return canonical;
            }
        }
        for (String canonical : values) {
            if (canonical.equalsIgnoreCase(input)) {
                return canonical;
            }
        }
        for (String canonical : values) {
            String display = valueDisplayNames.get(canonical);
            if (display != null && display.equalsIgnoreCase(input)) {
                return canonical;
            }
        }
        return null;
    }

    public Class<? extends Enum<?>> getEnumClass() {
        return enumClass;
    }

    public EnumDefinition expandWith(List<String> newValues, Map<String, Integer> newValueMap) {
        return expandWith(newValues, newValueMap, null);
    }

    public EnumDefinition expandWith(List<String> newValues, Map<String, Integer> newValueMap,
            Map<String, String> newValueDisplayNames) {
        if (newValues == null || newValues.isEmpty()) {
            return this;
        }

        List<String> mergedValues = new ArrayList<>(this.values);
        Map<String, Integer> mergedValueMap = new LinkedHashMap<>(this.valueMap);
        Map<String, String> mergedValueDisplayNames = new LinkedHashMap<>(this.valueDisplayNames);

        // Add new values skipping any duplicates
        for (String newValue : newValues) {
            if (!mergedValues.contains(newValue)) {
                mergedValues.add(newValue);

                // Add value mapping if provided
                if (newValueMap != null && newValueMap.containsKey(newValue)) {
                    mergedValueMap.put(newValue, newValueMap.get(newValue));
                } else {
                    // If no explicit mapping use next sequential value
                    int nextValue = mergedValueMap.isEmpty() ? 0
                            : Collections.max(mergedValueMap.values()) + 1;
                    mergedValueMap.put(newValue, nextValue);
                }

                if (newValueDisplayNames != null
                        && newValueDisplayNames.containsKey(newValue)) {
                    mergedValueDisplayNames.put(newValue, newValueDisplayNames.get(newValue));
                }
            }
        }

        return new EnumDefinition(this.name, mergedValues, mergedValueMap, this.enumClass,
                mergedValueDisplayNames);
    }

    @Override
    public String toString() {
        return name + values;
    }
}
