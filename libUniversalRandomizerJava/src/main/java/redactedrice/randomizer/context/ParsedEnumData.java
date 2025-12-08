package redactedrice.randomizer.context;

import java.util.*;

public class ParsedEnumData {
    private final List<String> valueNames;
    private final Map<String, Integer> valueMap;

    public ParsedEnumData(List<String> valueNames, Map<String, Integer> valueMap) {
        this.valueNames = Collections.unmodifiableList(new ArrayList<>(valueNames));
        this.valueMap =
                valueMap != null ? Collections.unmodifiableMap(new LinkedHashMap<>(valueMap))
                        : Collections.emptyMap();
    }

    public List<String> getValueNames() {
        return valueNames;
    }

    public Map<String, Integer> getValueMap() {
        return valueMap;
    }

    @Override
    public String toString() {
        return "ParsedEnumData{" + "valueNames=" + valueNames + ", valueMap=" + valueMap + "}";
    }
}
