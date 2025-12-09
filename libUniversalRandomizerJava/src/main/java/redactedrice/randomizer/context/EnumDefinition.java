package redactedrice.randomizer.context;

import java.util.*;

// holds an enum definition with its name and values
public class EnumDefinition {
    String name;
    List<String> values;
    Map<String, Integer> valueMap; // Maps enum name to integer value
    Class<? extends Enum<?>> enumClass;

    public EnumDefinition(String name, List<String> values, Map<String, Integer> valueMap,
            Class<? extends Enum<?>> enumClass) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
        this.valueMap =
                valueMap != null ? Collections.unmodifiableMap(valueMap) : Collections.emptyMap();
        this.enumClass = enumClass;
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

    public boolean hasValue(String value) {
        if (value == null) {
            return false;
        }
        // case-insensitive comparison for enum values
        // this allows lua scripts to use different casing
        for (String v : values) {
            if (v.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public Class<? extends Enum<?>> getEnumClass() {
        return enumClass;
    }

    public EnumDefinition expandWith(List<String> newValues, Map<String, Integer> newValueMap) {
        if (newValues == null || newValues.isEmpty()) {
            return this;
        }

        List<String> mergedValues = new ArrayList<>(this.values);
        Map<String, Integer> mergedValueMap = new LinkedHashMap<>(this.valueMap);

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
            }
        }

        // Return new EnumDefinition with merged values
        return new EnumDefinition(this.name, mergedValues, mergedValueMap, this.enumClass);
    }

    @Override
    public String toString() {
        return name + values;
    }
}
