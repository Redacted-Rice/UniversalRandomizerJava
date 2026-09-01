package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void optionalTablesIsNullWhenTheKeyIsMissing() {
        Map<String, Object> data = Map.of("cards", List.of(Map.of("id", "A")));
        assertEquals(1, ScriptTestValues.optionalTables(data, "cards").size());
        assertNull(ScriptTestValues.optionalTables(data, "original"));
        assertNull(ScriptTestValues.optionalTables(null, "cards"));
    }

    @Test
    void withoutKeyReturnsACopyWithoutThatEntry() {
        Map<String, Object> spec = Map.of("id", "A", "hp", 10);
        Map<String, Object> stripped = ScriptTestValues.withoutKey(spec, "id");
        assertEquals(Map.of("hp", 10), stripped);
        assertEquals("A", spec.get("id"));
        assertEquals(spec, ScriptTestValues.withoutKey(spec, "missing"));
    }

    @Test
    void optionalListOfMapsUnwrapsInlineListSpec() {
        List<Map<String, Object>> moves = ScriptTestValues.optionalListOfMaps(Map.of(
                "values", List.of(Map.of("name", "Splash"))), "moves");
        assertEquals("Splash", moves.get(0).get("name"));
        assertTrue(ScriptTestValues.optionalListOfMaps(Map.of(
                "values", Map.of()), "moves").isEmpty());
    }

    @Test
    void parseListFieldSpecDefaultsWholeAccessFromFieldName() {
        ScriptTestValues.ListFieldSpec moves = ScriptTestValues.parseListFieldSpec(Map.of(
                "values", List.of(Map.of("name", "Splash"))), "moves");
        assertEquals(ScriptTestValues.AccessType.WHOLE, moves.accessType());
        assertEquals("getMoves", moves.getterMethod());
        assertEquals("setMoves", moves.setterMethod());
        assertEquals("getMove", moves.itemGetterMethod());
        assertEquals("setMove", moves.itemSetterMethod());
        assertNull(moves.countGetterMethod());
        assertNull(moves.countSetterMethod());

        ScriptTestValues.ListFieldSpec tags = ScriptTestValues.parseListFieldSpec(Map.of(
                "values", List.of(Map.of("label", "veteran"))), "tags");
        assertEquals("getTags", tags.getterMethod());
        assertEquals("setTags", tags.setterMethod());
        assertEquals("getTag", tags.itemGetterMethod());
        assertEquals("setTag", tags.itemSetterMethod());
    }

    @Test
    void parseListFieldSpecDefaultsItemAccessFromFieldName() {
        ScriptTestValues.ListFieldSpec moves = ScriptTestValues.parseListFieldSpec(Map.of(
                "accessType", "item",
                "values", List.of(Map.of("name", "Splash"))), "moves");
        assertEquals(ScriptTestValues.AccessType.ITEM, moves.accessType());
        assertEquals("getMove", moves.getterMethod());
        assertEquals("setMove", moves.setterMethod());
        assertEquals("getNumMoves", moves.countGetterMethod());
        assertEquals("setNumMoves", moves.countSetterMethod());
    }

    @Test
    void parseListFieldSpecWholeIgnoresCountAccessors() {
        ScriptTestValues.ListFieldSpec whole = ScriptTestValues.parseListFieldSpec(Map.of(
                "countGetter", "getNumMoves",
                "countSetter", "setNumMoves",
                "values", List.of(Map.of("name", "Splash"))), "moves");
        assertNull(whole.countGetterMethod());
        assertNull(whole.countSetterMethod());
    }

    @Test
    void parseListFieldSpecOnlyOverridesNonCompliantAccessors() {
        ScriptTestValues.ListFieldSpec partial = ScriptTestValues.parseListFieldSpec(Map.of(
                "getter", "getAtRank",
                "values", List.of(Map.of("label", "captain"))), "moves");
        assertEquals("getAtRank", partial.getterMethod());
        assertEquals("setMoves", partial.setterMethod());
        assertNull(partial.countGetterMethod());
        assertNull(partial.countSetterMethod());

        ScriptTestValues.ListFieldSpec custom = ScriptTestValues.parseListFieldSpec(Map.of(
                "getter", "getAtRank",
                "setter", "setAtRank",
                "countGetter", "getRankCounts",
                "countSetter", "setRankCounts",
                "accessType", "item",
                "values", List.of(Map.of("label", "captain"))), "ranks");
        assertEquals("getAtRank", custom.getterMethod());
        assertEquals("setAtRank", custom.setterMethod());
        assertEquals("getRankCounts", custom.countGetterMethod());
        assertEquals("setRankCounts", custom.countSetterMethod());
    }

    @Test
    void parseKeyedMapSpecDefaultsWholeFromFieldName() {
        ScriptTestValues.KeyedMapSpec costs = ScriptTestValues.parseKeyedMapSpec(Map.of(
                "pre", List.of("clearCosts"),
                "WATER", 2), "costs");
        assertEquals(ScriptTestValues.AccessType.WHOLE, costs.accessType());
        assertEquals("getCosts", costs.getterMethod());
        assertEquals("setCosts", costs.setterMethod());
        assertEquals(List.of("clearCosts"), costs.pre());
        assertEquals(Map.of("WATER", 2), costs.entries());
    }

    @Test
    void parseKeyedMapSpecDefaultsItemFromFieldName() {
        ScriptTestValues.KeyedMapSpec costs = ScriptTestValues.parseKeyedMapSpec(Map.of(
                "accessType", "item",
                "pre", List.of("clearCosts"),
                "WATER", 2), "costs");
        assertEquals(ScriptTestValues.AccessType.ITEM, costs.accessType());
        assertEquals("getCost", costs.getterMethod());
        assertEquals("setCost", costs.setterMethod());
    }

    @Test
    void isKeyedMapSpecTreatsListValuesAndNestedObjectsSeparately() {
        assertTrue(ScriptTestValues.isKeyedMapSpec(Map.of(
                "accessType", "item",
                "pre", List.of("clearCosts"),
                "WATER", 2)));
        assertFalse(ScriptTestValues.isKeyedMapSpec(Map.of(
                "values", List.of(Map.of("name", "Splash")))));
        assertFalse(ScriptTestValues.isKeyedMapSpec(Map.of(
                "name", "Splash",
                "pre", "beforeHook")));
        assertFalse(ScriptTestValues.isKeyedMapSpec(Map.of(
                "setter", "setCosts",
                "values", List.of(Map.of("name", "Splash")))));
    }
}
