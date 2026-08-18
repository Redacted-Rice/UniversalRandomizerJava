package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ScriptTestValuesTest {

    @Test
    void requiredStringRejectsMissingAndBlank() {
        assertEquals("demo", ScriptTestValues.requiredString(Map.of("module", "demo"), "module"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.requiredString(Map.of(), "module"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.requiredString(Map.of("module", "  "), "module"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.requiredString(Map.of("module", 1), "module"));
    }

    @Test
    void optionalStringTreatsBlankAsMissing() {
        assertEquals("ok", ScriptTestValues.optionalString(Map.of("name", "ok"), "name"));
        assertNull(ScriptTestValues.optionalString(Map.of(), "name"));
        assertNull(ScriptTestValues.optionalString(Map.of("name", "  "), "name"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.optionalString(Map.of("name", 3), "name"));
    }

    @Test
    void listOfMapsRejectsEmptyAndNonTables() {
        List<Map<String, Object>> cards =
                ScriptTestValues.listOfMaps(List.of(Map.of("name", "A")), "cards");
        assertEquals(1, cards.size());
        assertEquals("A", cards.get(0).get("name"));

        assertTrue(ScriptTestValues.optionalListOfMaps(null, "cards").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.listOfMaps(List.of(), "cards"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.optionalListOfMaps("nope", "cards"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestValues.optionalListOfMaps(List.of("A"), "cards"));
    }

    @Test
    void optionalMapAndToIntRejectBadValues() {
        assertTrue(ScriptTestValues.optionalMap(null).isEmpty());
        assertEquals(2, ScriptTestValues.optionalMap(Map.of("count", 2)).get("count"));
        assertThrows(IllegalArgumentException.class, () -> ScriptTestValues.optionalMap("nope"));

        assertEquals(7, ScriptTestValues.toInt(null, 7));
        assertEquals(3, ScriptTestValues.toInt(3, 0));
        assertThrows(IllegalArgumentException.class, () -> ScriptTestValues.toInt("3", 0));
    }
}
