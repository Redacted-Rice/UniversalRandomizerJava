package redactedrice.randomizer.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EnumDisplayNameTest {

    @Test
    public void testEnumValueDisplayNamesAndResolution() {
        Map<String, String> displayNames = new LinkedHashMap<>();
        displayNames.put("MONSTER_FIRE", "Fire");
        displayNames.put("MONSTER_WATER", "Water");

        EnumDefinition def = new EnumDefinition("CardType",
                java.util.Arrays.asList("MONSTER_FIRE", "MONSTER_WATER"),
                Map.of("MONSTER_FIRE", 0, "MONSTER_WATER", 1), null, displayNames);

        assertEquals("Fire", def.getValueDisplayName("MONSTER_FIRE"));
        assertEquals("MONSTER_FIRE", def.resolveCanonicalValue("Fire"));
        assertEquals("MONSTER_WATER", def.resolveCanonicalValue("water"));
        assertEquals("MONSTER_FIRE", def.resolveCanonicalValue("MONSTER_FIRE"));
        assertNull(def.resolveCanonicalValue("Grass"));
    }

    @Test
    public void testExpandWithValueDisplayNames() {
        EnumDefinition original = new EnumDefinition("Test",
                java.util.Arrays.asList("A", "B"),
                Map.of("A", 0, "B", 1), null, Map.of("A", "Alpha"));

        EnumDefinition expanded = original.expandWith(java.util.Arrays.asList("C"), Map.of("C", 2),
                Map.of("C", "Charlie"));

        assertEquals("Alpha", expanded.getValueDisplayName("A"));
        assertEquals("Charlie", expanded.getValueDisplayName("C"));
        assertEquals("C", expanded.resolveCanonicalValue("Charlie"));
    }
}
