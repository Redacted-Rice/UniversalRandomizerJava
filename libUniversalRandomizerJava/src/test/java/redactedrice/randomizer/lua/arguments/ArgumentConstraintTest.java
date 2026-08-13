package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import redactedrice.randomizer.context.EnumDefinition;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentConstraintTest {

    @Test
    public void testAnyConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.any();
        assertEquals(ConstraintType.ANY, constraint.getType());
        assertTrue(constraint.validate(42, ArgumentType.INTEGER));
        assertTrue(constraint.validate("test", ArgumentType.STRING));
        assertNotNull(constraint.getDescription());
    }

    @Test
    public void testRangeConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        assertEquals(ConstraintType.RANGE, constraint.getType());
        assertEquals(1.0, constraint.getMin());
        assertEquals(100.0, constraint.getMax());
        assertNotNull(constraint.getDescription());

        assertTrue(constraint.validate(50, ArgumentType.INTEGER));
        assertTrue(constraint.validate(1, ArgumentType.INTEGER));
        assertTrue(constraint.validate(100, ArgumentType.INTEGER));
        assertFalse(constraint.validate(0, ArgumentType.INTEGER));
        assertFalse(constraint.validate(101, ArgumentType.INTEGER));
    }

    @Test
    public void testDiscreteRangeConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.discreteRange(0, 20, 5);
        assertEquals(ConstraintType.DISCRETE_RANGE, constraint.getType());
        assertEquals(5.0, constraint.getStep());

        assertTrue(constraint.validate(0, ArgumentType.INTEGER));
        assertTrue(constraint.validate(5, ArgumentType.INTEGER));
        assertTrue(constraint.validate(10, ArgumentType.INTEGER));
        assertTrue(constraint.validate(15, ArgumentType.INTEGER));
        assertTrue(constraint.validate(20, ArgumentType.INTEGER));
        assertFalse(constraint.validate(7, ArgumentType.INTEGER));
        assertFalse(constraint.validate(22, ArgumentType.INTEGER));
    }

    @Test
    public void testEnumConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.enumValues(Arrays.asList("A", "B", "C"));
        assertEquals(ConstraintType.ENUM, constraint.getType());
        assertNotNull(constraint.getDescription());
        assertNotNull(constraint.getAllowedValues());
        assertEquals(3, constraint.getAllowedValues().size());

        assertTrue(constraint.validate("A", ArgumentType.STRING));
        assertTrue(constraint.validate("B", ArgumentType.STRING));
        assertTrue(constraint.validate("C", ArgumentType.STRING));
        assertFalse(constraint.validate("D", ArgumentType.STRING));
    }

    @Test
    public void testEnumConstraintNumericEquivalence() {
        ArgumentConstraint constraint = ArgumentConstraint.enumValues(Arrays.asList(1, 2, 3));

        assertTrue(constraint.validate(2, ArgumentType.INTEGER));
        assertTrue(constraint.validate(2.0, ArgumentType.INTEGER));
        assertTrue(constraint.validate(2.0, ArgumentType.DOUBLE));
        assertFalse(constraint.validate(2.5, ArgumentType.DOUBLE));
        assertFalse(constraint.validate(4, ArgumentType.INTEGER));
    }

    @Test
    public void testEnumExclusions() {
        ArgumentConstraint constraint =
                ArgumentConstraint.enumExclusions(Arrays.asList("COLORLESS", "UNUSED_TYPE"));
        assertEquals(ConstraintType.ENUM, constraint.getType());
        assertEquals(2, constraint.getExcludedValues().size());
        assertTrue(constraint.validate("FIRE", ArgumentType.ENUM));
        assertFalse(constraint.validate("COLORLESS", ArgumentType.ENUM));
        assertFalse(constraint.validate("UNUSED_TYPE", ArgumentType.ENUM));

        assertEquals(Arrays.asList("FIRE", "WATER"),
                constraint.filterEnumValues(Arrays.asList("FIRE", "COLORLESS", "WATER", "UNUSED_TYPE")));
    }

    @Test
    public void testEnumExclusionsMatchDisplayNamesAndCase() {
        EnumDefinition enumDef = new EnumDefinition("EnergyType",
                Arrays.asList("FIRE", "WATER", "COLORLESS"),
                Map.of("FIRE", 0, "WATER", 1, "COLORLESS", 2), null,
                Map.of("FIRE", "Fire", "WATER", "Water", "COLORLESS", "Colorless"));

        ArgumentConstraint byDisplay =
                ArgumentConstraint.enumExclusions(Arrays.asList("Colorless", "fire"));
        assertFalse(byDisplay.validate("COLORLESS", ArgumentType.ENUM, enumDef));
        assertFalse(byDisplay.validate("FIRE", ArgumentType.ENUM, enumDef));
        assertTrue(byDisplay.validate("WATER", ArgumentType.ENUM, enumDef));
        assertEquals(Arrays.asList("WATER"),
                byDisplay.filterEnumValues(Arrays.asList("FIRE", "WATER", "COLORLESS"), enumDef));

        ArgumentConstraint byCase = ArgumentConstraint.enumExclusions(Arrays.asList("colorless"));
        assertFalse(byCase.validate("COLORLESS", ArgumentType.ENUM));
        assertFalse(byCase.validate("COLORLESS", ArgumentType.ENUM, enumDef));
    }
}
