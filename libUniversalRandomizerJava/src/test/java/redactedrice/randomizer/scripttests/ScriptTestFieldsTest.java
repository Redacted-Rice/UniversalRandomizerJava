package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redactedrice.randomizer.context.JavaContext;

class ScriptTestFieldsTest {

    enum Kind {
        FIRE, WATER, COLORLESS
    }

    static class Label {
        private String text = "";

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    static class Attack {
        public Label name = new Label();
        public byte damage;
        private final EnumMap<Kind, Byte> energyCost = new EnumMap<>(Kind.class);

        public Attack copy() {
            Attack copy = new Attack();
            copy.name.setText(name.toString());
            copy.damage = damage;
            copy.energyCost.putAll(energyCost);
            return copy;
        }

        public void clearCosts() {
            energyCost.clear();
        }

        public void setCost(Kind kind, byte cost) {
            energyCost.put(kind, cost);
        }

        public byte getCost(Kind kind) {
            Byte cost = energyCost.get(kind);
            return cost == null ? 0 : cost;
        }
    }

    static class Unit {
        public Label name = new Label();
        public Kind type = Kind.COLORLESS;
        private int hp;
        private int numMoves;
        private final Attack[] moves = { new Attack(), new Attack() };

        public int getHp() {
            return hp;
        }

        public boolean setHp(int hp) {
            this.hp = hp;
            return true;
        }

        public int getNumMoves() {
            return numMoves;
        }

        public boolean setNumMoves(int numMoves) {
            this.numMoves = numMoves;
            return true;
        }

        public Attack getMove(int index) {
            return moves[index].copy();
        }

        public boolean setMove(Attack move, int index, boolean force) {
            moves[index] = move.copy();
            return force || true;
        }
    }

    static class NoListAccess {
        public String name = "plain";
    }

    static class SetTypeOnly {
        private Kind type = Kind.COLORLESS;

        public Kind getType() {
            return type;
        }

        public void setType(Kind type) {
            this.type = type;
        }
    }

    private JavaContext context;

    @BeforeEach
    void setUp() {
        context = new JavaContext();
        context.registerEnum(Kind.class);
    }

    @Test
    void applyWritesSettersPublicFieldsNestedListsAndDynamicValues() {
        Unit unit = appliedUnit();

        assertEquals("Ember", unit.name.toString());
        assertEquals(40, unit.getHp());
        assertEquals(Kind.FIRE, unit.type);
        assertEquals(3, context.wrap(unit).get("evoLineId").toint());
        assertEquals(1, unit.getNumMoves());
        assertEquals("Splash", unit.moves[0].name.toString());
        assertEquals(20, unit.moves[0].damage);
        assertEquals(2, unit.moves[0].getCost(Kind.WATER));
        assertEquals(1, unit.moves[0].getCost(Kind.COLORLESS));
        assertEquals(0, unit.moves[0].getCost(Kind.FIRE));
        assertTrue(context.wrap(unit).get("kindTag").isstring());
        assertEquals("FIRE", context.wrap(unit).get("kindTag").tojstring());
    }

    @Test
    void applyCoercesProvidedDynamicEnumFields() {
        context.registerDynamicField("maxStage", "Kind");
        Unit unit = new Unit();
        ScriptTestFields.apply(context, unit, Map.of("maxStage", "WATER"));

        assertEquals(Kind.WATER, context.wrap(unit).get("maxStage").touserdata());
    }

    @Test
    void applyLeavesUnknownProvidedEnumNamesAsStrings() {
        context.registerDynamicField("maxStage", "Kind");
        Unit unit = new Unit();
        ScriptTestFields.apply(context, unit, Map.of("maxStage", "GRASS"));

        assertTrue(context.wrap(unit).get("maxStage").isstring());
        assertEquals("GRASS", context.wrap(unit).get("maxStage").tojstring());
    }

    @Test
    void applyCoercesEnumSetterArguments() {
        SetTypeOnly unit = new SetTypeOnly();
        ScriptTestFields.apply(context, unit, Map.of("type", "FIRE"));
        assertEquals(Kind.FIRE, unit.getType());
    }

    @Test
    void applyEmptyOrNullSpecDoesNothing() {
        Unit unit = new Unit();
        unit.setHp(10);

        ScriptTestFields.apply(context, unit, Map.of());
        ScriptTestFields.apply(context, unit, null);

        assertEquals(10, unit.getHp());
        assertEquals("", unit.name.toString());
    }

    @Test
    void applyListWithoutGetterFails() {
        NoListAccess target = new NoListAccess();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ScriptTestFields.apply(context, target,
                        Map.of("moves", moveList(Map.of("name", "Splash")))));
        assertTrue(error.getMessage().contains("No getMove"), error.getMessage());
    }

    @Test
    void applyLeavesStringFieldsAloneEvenWhenTheyMatchAnEnumName() {
        NoListAccess target = new NoListAccess();
        ScriptTestFields.apply(context, target, Map.of("name", "FIRE"));
        assertEquals("FIRE", target.name);
    }

    @Test
    void applyCoercesPublicEnumFieldRegisteredUnderACustomName() {
        JavaContext custom = new JavaContext();
        custom.registerEnum("EE_Kinds", Kind.class);
        Unit unit = new Unit();

        ScriptTestFields.apply(custom, unit, Map.of("type", "FIRE"));

        assertEquals(Kind.FIRE, unit.type);
    }

    @Test
    void collectMismatchesPassesWhenExpectMatches() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, matchingExpect());
        assertTrue(mismatches.isEmpty(), mismatches.toString());
    }

    @Test
    void collectMismatchesReportsWrongScalarAndDynamicFields() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("hp", 99, "evoLineId", 1));

        assertHas(mismatches, "unit hp expected 99 but was 40");
        assertHas(mismatches, "unit evoLineId expected 1 but was 3");
        assertEquals(2, mismatches.size(), mismatches.toString());
    }

    @Test
    void collectMismatchesReportsWrongEnumAndSetText() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("type", "WATER", "name", "Flare"));

        assertHas(mismatches, "unit type expected WATER but was FIRE");
        assertHas(mismatches, "unit name expected Flare but was Ember");
    }

    @Test
    void collectMismatchesReportsMissingDynamicField() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("evoLineMaxStage", "STAGE_1"));

        assertHas(mismatches, "unit evoLineMaxStage expected STAGE_1 but was nil");
    }

    @Test
    void collectMismatchesReportsMoveCount() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("moves", moveList(
                Map.of("name", "Splash"),
                Map.of("name", "Flare"))));

        assertEquals(List.of("unit moves count expected 2 but was 1"), mismatches);
    }

    @Test
    void collectMismatchesReportsWrongNestedMoveFields() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("moves", moveList(Map.of(
                "name", "Flare",
                "damage", 99))));

        assertHas(mismatches, "unit moves[1] name expected Flare but was Splash");
        assertHas(mismatches, "unit moves[1] damage expected 99 but was 20");
    }

    @Test
    void collectMismatchesTreatsOmittedCostsAsZero() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("moves", moveList(Map.of(
                "costs", costs(2, null)))));

        assertHas(mismatches, "unit moves[1] costs COLORLESS expected 0 but was 1");
        assertTrue(mismatches.stream().noneMatch(m -> m.contains("FIRE")), mismatches.toString());
        assertTrue(mismatches.stream().noneMatch(m -> m.contains("WATER expected")),
                mismatches.toString());
    }

    @Test
    void collectMismatchesReportsWrongCostValues() {
        Unit unit = appliedUnit();
        List<String> mismatches = mismatches(unit, Map.of("moves", moveList(Map.of(
                "costs", costs(9, 1)))));

        assertHas(mismatches, "unit moves[1] costs WATER expected 9 but was 2");
        assertTrue(mismatches.stream().noneMatch(m -> m.contains("COLORLESS expected")),
                mismatches.toString());
    }

    @Test
    void collectMismatchesNullSpecDoesNothing() {
        Unit unit = appliedUnit();
        List<String> mismatches = new ArrayList<>();
        ScriptTestFields.collectMismatches(context, unit, null, mismatches, "unit");
        assertTrue(mismatches.isEmpty());
    }

    @Test
    void collectWholeListUsesItemGetter() {
        Unit unit = new Unit();
        unit.moves[0].name.setText("Splash");
        unit.moves[0].damage = 20;
        unit.moves[1].name.setText("Flare");
        unit.moves[1].damage = 30;
        unit.setNumMoves(2);

        Map<String, Object> movesSpec = new LinkedHashMap<>();
        movesSpec.put("accessType", "whole");
        movesSpec.put("values", List.of(
                Map.of("name", "Splash", "damage", 20),
                Map.of("name", "Flare", "damage", 30)));

        List<String> mismatches = new ArrayList<>();
        ScriptTestFields.collectMismatches(context, unit, Map.of("moves", movesSpec),
                mismatches, "unit");
        assertTrue(mismatches.isEmpty(), mismatches.toString());

        unit.moves[1].name.setText("wrong");
        mismatches = new ArrayList<>();
        ScriptTestFields.collectMismatches(context, unit, Map.of("moves", movesSpec),
                mismatches, "unit");
        assertHas(mismatches, "unit moves[2] name expected Flare but was wrong");
    }

    @Test
    void failIfMismatchesThrowsTheJoinedMessage() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ScriptTestFields.failIfMismatches("case", List.of("a", "b")));
        assertEquals("case a. b", error.getMessage());
        ScriptTestFields.failIfMismatches("case", List.of());
    }

    private Unit appliedUnit() {
        Unit unit = new Unit();
        ScriptTestFields.apply(context, unit, Map.of(
                "name", "Ember",
                "hp", 40,
                "type", "FIRE",
                "evoLineId", 3,
                "kindTag", "FIRE",
                "moves", moveList(Map.of(
                        "name", "Splash",
                        "damage", 20,
                        "costs", costs(2, 1)))));
        return unit;
    }

    private static Map<String, Object> matchingExpect() {
        return Map.of(
                "name", "Ember",
                "hp", 40,
                "type", "FIRE",
                "evoLineId", 3,
                "moves", moveList(Map.of(
                        "name", "Splash",
                        "damage", 20,
                        "costs", costs(2, 1))));
    }

    private static Map<String, Object> moveList(Map<String, Object>... entries) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("accessType", "item");
        spec.put("values", List.of(entries));
        return spec;
    }

    private static Map<String, Object> costs(Integer water, Integer colorless) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("accessType", "item");
        spec.put("pre", "clearCosts");
        if (water != null) {
            spec.put("WATER", water);
        }
        if (colorless != null) {
            spec.put("COLORLESS", colorless);
        }
        return spec;
    }

    private List<String> mismatches(Unit unit, Map<String, Object> expect) {
        List<String> mismatches = new ArrayList<>();
        ScriptTestFields.collectMismatches(context, unit, expect, mismatches, "unit");
        return mismatches;
    }

    private static void assertHas(List<String> mismatches, String expected) {
        assertTrue(mismatches.contains(expected),
                "missing '" + expected + "' in " + mismatches);
    }
}
