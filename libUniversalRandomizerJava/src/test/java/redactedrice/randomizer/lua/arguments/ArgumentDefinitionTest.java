package redactedrice.randomizer.lua.arguments;

import redactedrice.randomizer.context.EnumRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentDefinitionTest {

    @Test
    public void testConstruction() {
        TypeDefinition stringType = TypeDefinition.string();
        ArgumentDefinition required = new ArgumentDefinition("name", stringType, null);
        assertEquals("name", required.getName());
        assertEquals(stringType, required.getTypeDefinition());
        assertNull(required.getDefaultValue());

        TypeDefinition intType = TypeDefinition.integer();
        ArgumentDefinition withDefault = new ArgumentDefinition("level", intType, 50);
        assertEquals("level", withDefault.getName());
        assertEquals(50, withDefault.getDefaultValue());
    }

    @Test
    public void testConvertAndValidatePrimitiveTypes() {
        ArgumentDefinition intArg =
                new ArgumentDefinition("value", TypeDefinition.integer(), null);
        assertEquals(42, intArg.convertAndValidate(42, null));

        ArgumentDefinition stringArg =
                new ArgumentDefinition("name", TypeDefinition.string(), null);
        assertEquals("test", stringArg.convertAndValidate("test", null));

        ArgumentDefinition doubleArg =
                new ArgumentDefinition("value", TypeDefinition.doubleType(), null);
        assertEquals(42.5, doubleArg.convertAndValidate(42.5, null));

        ArgumentDefinition boolArg = new ArgumentDefinition("flag", TypeDefinition.bool(), null);
        assertEquals(true, boolArg.convertAndValidate(true, null));
    }

    @Test
    public void testConvertAndValidateWithConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, null);

        assertEquals(50, argDef.convertAndValidate(50, null));
        assertThrows(IllegalArgumentException.class, () -> argDef.convertAndValidate(150, null));
    }

    @Test
    public void testConvertAndValidateEnum() {
        EnumRegistry enumContext = new EnumRegistry();
        enumContext.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        TypeDefinition enumType = TypeDefinition.enumType("Difficulty");
        ArgumentDefinition argDef = new ArgumentDefinition("difficulty", enumType, null);

        assertEquals("EASY", argDef.convertAndValidate("EASY", enumContext));
        assertThrows(IllegalArgumentException.class,
                () -> argDef.convertAndValidate("INVALID", enumContext));
    }

    @Test
    public void testConvertAndValidateListAndMap() {
        ArgumentDefinition listArg = new ArgumentDefinition("values",
                TypeDefinition.listOf(TypeDefinition.integer()), null);
        Object listResult = listArg.convertAndValidate(Arrays.asList(1, 2, 3), null);
        assertNotNull(listResult);
        assertTrue(listResult instanceof java.util.List);

        ArgumentDefinition mapArg = new ArgumentDefinition("mapping",
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()), null);
        java.util.Map<String, Integer> input = new java.util.HashMap<>();
        input.put("key1", 1);
        input.put("key2", 2);
        Object mapResult = mapArg.convertAndValidate(input, null);
        assertNotNull(mapResult);
        assertTrue(mapResult instanceof java.util.Map);
    }

    @Test
    public void testDefaultValueHandling() {
        ArgumentDefinition intDefault = new ArgumentDefinition("level",
                TypeDefinition.integer(), 50);
        assertEquals(50, intDefault.convertAndValidate(null, null));
        assertTrue(intDefault.validate(null, null));

        ArgumentDefinition stringDefault = new ArgumentDefinition("name",
                TypeDefinition.string(), "default");
        assertEquals("default", stringDefault.convertAndValidate(null, null));
    }

    @Test
    public void testMissingRequiredArgument() {
        ArgumentDefinition required = new ArgumentDefinition("name", TypeDefinition.string(), null);
        assertThrows(IllegalArgumentException.class, () -> required.convertAndValidate(null, null));
        assertFalse(required.validate(null, null));
    }

    @Test
    public void testConstructorRejectsInvalidArguments() {
        TypeDefinition typeDef = TypeDefinition.string();
        assertThrows(IllegalArgumentException.class, () -> new ArgumentDefinition(null, typeDef, null));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentDefinition("", typeDef, null));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentDefinition("   ", typeDef, null));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentDefinition("name", null, null));
    }

    @Test
    public void testValidate() {
        ArgumentDefinition intArg = new ArgumentDefinition("value", TypeDefinition.integer(), null);
        assertTrue(intArg.validate(42, null));
        assertFalse(intArg.validate("invalid", null));

        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        ArgumentDefinition constrained = new ArgumentDefinition("level",
                TypeDefinition.integer(constraint), null);
        assertTrue(constrained.validate(50, null));
        assertFalse(constrained.validate(150, null));
        assertFalse(constrained.validate(null, null));

        EnumRegistry enumContext = new EnumRegistry();
        enumContext.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));
        ArgumentDefinition enumArg = new ArgumentDefinition("difficulty",
                TypeDefinition.enumType("Difficulty"), null);
        assertTrue(enumArg.validate("EASY", enumContext));
        assertFalse(enumArg.validate("INVALID", enumContext));
        assertFalse(enumArg.validate(null, enumContext));
    }

    @Test
    public void testGetConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        ArgumentDefinition constrained = new ArgumentDefinition("level",
                TypeDefinition.integer(constraint), null);
        assertEquals(constraint, constrained.getConstraint());

        ArgumentDefinition unconstrained = new ArgumentDefinition("value",
                TypeDefinition.integer(), null);
        ArgumentConstraint anyConstraint = unconstrained.getConstraint();
        assertNotNull(anyConstraint);
        assertEquals(ConstraintType.ANY, anyConstraint.getType());

        // Display/API exposes enforced constraints (ignored declarations become ANY)
        ArgumentDefinition boolWithRange = new ArgumentDefinition("flag",
                TypeDefinition.bool(ArgumentConstraint.range(0, 1)), null);
        assertEquals(ConstraintType.ANY, boolWithRange.getConstraint().getType());

        ArgumentDefinition stringWithRange = new ArgumentDefinition("name",
                TypeDefinition.string(ArgumentConstraint.range(1, 10)), null);
        assertEquals(ConstraintType.ANY, stringWithRange.getConstraint().getType());
    }

    @Test
    public void testDisplayName() {
        ArgumentDefinition labeled = new ArgumentDefinition("numMoves", "Number of moves",
                TypeDefinition.integer(), 2);
        assertEquals("numMoves", labeled.getName());
        assertEquals("Number of moves", labeled.getDisplayName());
        assertEquals("Number of moves", labeled.getRegisteredDisplayName());

        ArgumentDefinition unlabeled = new ArgumentDefinition("numMoves", TypeDefinition.integer(),
                2);
        assertEquals("numMoves", unlabeled.getDisplayName());
        assertNull(unlabeled.getRegisteredDisplayName());
    }

    @Test
    public void testToString() {
        ArgumentDefinition withDefault = new ArgumentDefinition("level",
                TypeDefinition.integer(), 50);
        String defaultText = withDefault.toString();
        assertTrue(defaultText.contains("ArgumentDefinition"));
        assertTrue(defaultText.contains("name='level'"));
        assertTrue(defaultText.contains("type=Integer"));
        assertTrue(defaultText.contains("default=50"));

        ArgumentDefinition withConstraint = new ArgumentDefinition("level",
                TypeDefinition.integer(ArgumentConstraint.range(1, 100)), null);
        assertTrue(withConstraint.toString().contains("constraint="));
    }
}
