package redactedrice.randomizer.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class EnumDisplayNamesValidator {

    private EnumDisplayNamesValidator() {}

    static Map<String, String> validate(List<String> canonicalValues,
            Map<String, String> valueDisplayNames, String enumName) {
        if (valueDisplayNames == null || valueDisplayNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> canonicalSet = new HashSet<>(canonicalValues);
        Map<String, String> normalized = new LinkedHashMap<>();
        Map<String, String> labelsSeen = new HashMap<>();

        for (Map.Entry<String, String> entry : valueDisplayNames.entrySet()) {
            String canonical = entry.getKey();
            if (!canonicalSet.contains(canonical)) {
                throw new IllegalArgumentException(String.format(
                        "Enum '%s' displayNames key '%s' is not a registered value. Values: %s",
                        enumName, canonical, canonicalValues));
            }

            String display = entry.getValue();
            if (display == null || display.isBlank()) {
                continue;
            }

            String labelKey = display.toLowerCase(Locale.ROOT);
            String otherCanonical = labelsSeen.get(labelKey);
            if (otherCanonical != null) {
                throw new IllegalArgumentException(String.format(
                        "Enum '%s' has duplicate display label '%s' for %s and %s", enumName,
                        display, otherCanonical, canonical));
            }
            labelsSeen.put(labelKey, canonical);
            normalized.put(canonical, display.trim());
        }

        return Collections.unmodifiableMap(normalized);
    }
}
