package redactedrice.randomizer.metadata;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentConstraintTest {

    @Test
    public void testAnyConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.any();
        assertEquals(ArgumentConstraint.ConstraintType.ANY, constraint.getType());
        assertTrue(constraint.validate(42, ArgumentType.INTEGER));
        assertTrue(constraint.validate("test", ArgumentType.STRING));
    }

    @Test
    public void testRangeConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        assertEquals(ArgumentConstraint.ConstraintType.RANGE, constraint.getType());
        assertEquals(1.0, constraint.getMin());
        assertEquals(100.0, constraint.getMax());

        assertTrue(constraint.validate(50, ArgumentType.INTEGER));
        assertTrue(constraint.validate(1, ArgumentType.INTEGER));
        assertTrue(constraint.validate(100, ArgumentType.INTEGER));
        assertFalse(constraint.validate(0, ArgumentType.INTEGER));
        assertFalse(constraint.validate(101, ArgumentType.INTEGER));
    }

    @Test
    public void testDiscreteRangeConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.discreteRange(0, 20, 5);
        assertEquals(ArgumentConstraint.ConstraintType.DISCRETE_RANGE, constraint.getType());
        assertEquals(5.0, constraint.getStep());

        // valid values
        assertTrue(constraint.validate(0, ArgumentType.INTEGER));
        assertTrue(constraint.validate(5, ArgumentType.INTEGER));
        assertTrue(constraint.validate(10, ArgumentType.INTEGER));
        assertTrue(constraint.validate(15, ArgumentType.INTEGER));
        assertTrue(constraint.validate(20, ArgumentType.INTEGER));
        // invalid - not on step boundary
        assertFalse(constraint.validate(7, ArgumentType.INTEGER));
        assertFalse(constraint.validate(22, ArgumentType.INTEGER));
    }

    @Test
    public void testEnumConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.enumValues(Arrays.asList("A", "B", "C"));
        assertEquals(ArgumentConstraint.ConstraintType.ENUM, constraint.getType());

        assertTrue(constraint.validate("A", ArgumentType.STRING));
        assertTrue(constraint.validate("B", ArgumentType.STRING));
        assertTrue(constraint.validate("C", ArgumentType.STRING));
        assertFalse(constraint.validate("D", ArgumentType.STRING));
    }

    @Test
    public void testGetAllowedValues() {
        ArgumentConstraint constraint = ArgumentConstraint.enumValues(Arrays.asList("A", "B", "C"));
        assertNotNull(constraint.getAllowedValues());
        assertEquals(3, constraint.getAllowedValues().size());
    }

    @Test
    public void testGetDescription() {
        ArgumentConstraint any = ArgumentConstraint.any();
        assertNotNull(any.getDescription());

        ArgumentConstraint range = ArgumentConstraint.range(1, 100);
        assertNotNull(range.getDescription());

        ArgumentConstraint enumConst = ArgumentConstraint.enumValues(Arrays.asList("A", "B"));
        assertNotNull(enumConst.getDescription());
    }
}

