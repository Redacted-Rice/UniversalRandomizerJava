package redactedrice.randomizer.context;

import java.util.*;

public class ParsedEnumData {
    private final List<String> valueNames;
    private final Map<String, Integer> valueMap;
    private final Map<String, String> valueDisplayNames;

    public ParsedEnumData(List<String> valueNames, Map<String, Integer> valueMap) {
        this(valueNames, valueMap, null);
    }

    public ParsedEnumData(List<String> valueNames, Map<String, Integer> valueMap,
            Map<String, String> valueDisplayNames) {
        this.valueNames = Collections.unmodifiableList(new ArrayList<>(valueNames));
        this.valueMap =
                valueMap != null ? Collections.unmodifiableMap(new LinkedHashMap<>(valueMap))
                        : Collections.emptyMap();
        this.valueDisplayNames = valueDisplayNames != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(valueDisplayNames))
                : Collections.emptyMap();
    }

    public List<String> getValueNames() {
        return valueNames;
    }

    public Map<String, Integer> getValueMap() {
        return valueMap;
    }

    public Map<String, String> getValueDisplayNames() {
        return valueDisplayNames;
    }

    @Override
    public String toString() {
        return "ParsedEnumData{" + "valueNames=" + valueNames + ", valueMap=" + valueMap
                + ", valueDisplayNames=" + valueDisplayNames + '}';
    }
}
