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

    @Test
    public void testExpandWithRejectsDuplicateDisplayLabels() {
        EnumDefinition original = new EnumDefinition("Test",
                java.util.Arrays.asList("A", "B"),
                Map.of("A", 0, "B", 1), null, Map.of("A", "Alpha"));

        assertThrows(IllegalArgumentException.class,
                () -> original.expandWith(java.util.Arrays.asList("C"), Map.of("C", 2),
                        Map.of("C", "alpha")));
    }

    @Test
    public void testExactDisplayNameWinsOverCaseInsensitiveCanonical() {
        // FIRE labeled "Water" should still resolve from the exact label, not get swallowed by WATER
        EnumDefinition def = new EnumDefinition("EnergyType",
                java.util.Arrays.asList("FIRE", "WATER"),
                Map.of("FIRE", 0, "WATER", 1), null, Map.of("FIRE", "Water"));

        assertEquals("FIRE", def.resolveCanonicalValue("Water"));
        assertEquals("WATER", def.resolveCanonicalValue("WATER"));
        assertEquals("WATER", def.resolveCanonicalValue("water"));
    }

    @Test
    public void testRejectsUnknownDisplayNameKey() {
        Map<String, String> displayNames = Map.of("FIRE", "Fire", "GRASS", "Grass");
        assertThrows(IllegalArgumentException.class,
                () -> new EnumDefinition("EnergyType", java.util.Arrays.asList("FIRE", "WATER"),
                        Map.of("FIRE", 0, "WATER", 1), null, displayNames));
    }

    @Test
    public void testRejectsDuplicateDisplayLabels() {
        Map<String, String> displayNames = Map.of("FIRE", "Fire", "WATER", "fire");
        assertThrows(IllegalArgumentException.class,
                () -> new EnumDefinition("EnergyType", java.util.Arrays.asList("FIRE", "WATER"),
                        Map.of("FIRE", 0, "WATER", 1), null, displayNames));
    }
}
