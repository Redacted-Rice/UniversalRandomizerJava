package redactedrice.randomizer.context;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Override
    public String toString() {
        return name + values;
    }
}
