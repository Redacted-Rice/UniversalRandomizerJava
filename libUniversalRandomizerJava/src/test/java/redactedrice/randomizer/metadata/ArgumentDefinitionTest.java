package redactedrice.randomizer.metadata;

import redactedrice.randomizer.context.EnumContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentDefinitionTest {

    @Test
    public void testStringArgument() {
        TypeDefinition typeDef = TypeDefinition.string();
        ArgumentDefinition argDef = new ArgumentDefinition("name", typeDef, null);
        assertEquals("name", argDef.getName());
        assertEquals(typeDef, argDef.getTypeDefinition());
        assertNull(argDef.getDefaultValue());
    }

    @Test
    public void testArgumentWithDefault() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, 50);
        assertEquals("level", argDef.getName());
        assertEquals(50, argDef.getDefaultValue());
    }

    @Test
    public void testConvertAndValidateInteger() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("value", typeDef, null);
        Object result = argDef.convertAndValidate(42, null);
        assertEquals(42, result);
    }

    @Test
    public void testConvertAndValidateString() {
        TypeDefinition typeDef = TypeDefinition.string();
        ArgumentDefinition argDef = new ArgumentDefinition("name", typeDef, null);
        Object result = argDef.convertAndValidate("test", null);
        assertEquals("test", result);
    }

    @Test
    public void testConvertAndValidateWithConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, null);

        Object result = argDef.convertAndValidate(50, null);
        assertEquals(50, result);

        assertThrows(IllegalArgumentException.class, () -> {
            argDef.convertAndValidate(150, null);
        });
    }

    @Test
    public void testConvertAndValidateEnum() {
        EnumContext enumContext = new EnumContext();
        enumContext.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        TypeDefinition enumType = TypeDefinition.enumType("Difficulty");
        ArgumentDefinition argDef = new ArgumentDefinition("difficulty", enumType, null);

        Object result = argDef.convertAndValidate("EASY", enumContext);
        assertEquals("EASY", result);

        assertThrows(IllegalArgumentException.class, () -> {
            argDef.convertAndValidate("INVALID", enumContext);
        });
    }

    @Test
    public void testConvertAndValidateList() {
        TypeDefinition listType = TypeDefinition.listOf(TypeDefinition.integer());
        ArgumentDefinition argDef = new ArgumentDefinition("values", listType, null);

        java.util.List<Integer> input = java.util.Arrays.asList(1, 2, 3);
        Object result = argDef.convertAndValidate(input, null);
        assertNotNull(result);
        assertTrue(result instanceof java.util.List);
    }

    @Test
    public void testUseDefaultValue() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, 50);
        Object result = argDef.convertAndValidate(null, null);
        assertEquals(50, result);
    }

    @Test
    public void testMissingRequiredArgument() {
        TypeDefinition typeDef = TypeDefinition.string();
        ArgumentDefinition argDef = new ArgumentDefinition("name", typeDef, null);
        assertThrows(IllegalArgumentException.class, () -> {
            argDef.convertAndValidate(null, null);
        });
    }

    @Test
    public void testConstructorNullNameThrows() {
        TypeDefinition typeDef = TypeDefinition.string();
        assertThrows(IllegalArgumentException.class, () -> {
            new ArgumentDefinition(null, typeDef, null);
        });
    }

    @Test
    public void testConstructorEmptyNameThrows() {
        TypeDefinition typeDef = TypeDefinition.string();
        assertThrows(IllegalArgumentException.class, () -> {
            new ArgumentDefinition("", typeDef, null);
        });
    }

    @Test
    public void testConstructorWhitespaceNameThrows() {
        TypeDefinition typeDef = TypeDefinition.string();
        assertThrows(IllegalArgumentException.class, () -> {
            new ArgumentDefinition("   ", typeDef, null);
        });
    }

    @Test
    public void testConstructorNullTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ArgumentDefinition("name", null, null);
        });
    }

    @Test
    public void testValidate() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("value", typeDef, null);

        assertTrue(argDef.validate(42, null));
        assertFalse(argDef.validate(null, null));
        assertFalse(argDef.validate("invalid", null));
    }

    @Test
    public void testValidateWithDefault() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("value", typeDef, 50);

        // Should return true for null when default exists
        assertTrue(argDef.validate(null, null));
        assertTrue(argDef.validate(42, null));
    }

    @Test
    public void testValidateWithConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, null);

        assertTrue(argDef.validate(50, null));
        assertFalse(argDef.validate(150, null));
        assertFalse(argDef.validate(null, null));
    }

    @Test
    public void testValidateWithEnum() {
        EnumContext enumContext = new EnumContext();
        enumContext.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        TypeDefinition enumType = TypeDefinition.enumType("Difficulty");
        ArgumentDefinition argDef = new ArgumentDefinition("difficulty", enumType, null);

        assertTrue(argDef.validate("EASY", enumContext));
        assertFalse(argDef.validate("INVALID", enumContext));
        assertFalse(argDef.validate(null, enumContext));
    }

    @Test
    public void testGetConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, null);

        assertEquals(constraint, argDef.getConstraint());
    }

    @Test
    public void testGetConstraintNoConstraint() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("value", typeDef, null);

        ArgumentConstraint constraint = argDef.getConstraint();
        assertNotNull(constraint);
        assertEquals(ArgumentConstraint.ConstraintType.ANY, constraint.getType());
    }

    @Test
    public void testToString() {
        TypeDefinition typeDef = TypeDefinition.integer();
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, 50);

        String str = argDef.toString();
        assertTrue(str.contains("ArgumentDefinition"));
        assertTrue(str.contains("name='level'"));
        assertTrue(str.contains("type=Integer"));
        assertTrue(str.contains("default=50"));
    }

    @Test
    public void testToStringWithConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        ArgumentDefinition argDef = new ArgumentDefinition("level", typeDef, null);

        String str = argDef.toString();
        assertTrue(str.contains("constraint="));
    }

    @Test
    public void testConvertAndValidateDouble() {
        TypeDefinition typeDef = TypeDefinition.doubleType();
        ArgumentDefinition argDef = new ArgumentDefinition("value", typeDef, null);
        Object result = argDef.convertAndValidate(42.5, null);
        assertEquals(42.5, result);
    }

    @Test
    public void testConvertAndValidateBoolean() {
        TypeDefinition typeDef = TypeDefinition.bool();
        ArgumentDefinition argDef = new ArgumentDefinition("flag", typeDef, null);
        Object result = argDef.convertAndValidate(true, null);
        assertEquals(true, result);
    }

    @Test
    public void testConvertAndValidateMap() {
        TypeDefinition mapType =
                TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer());
        ArgumentDefinition argDef = new ArgumentDefinition("mapping", mapType, null);

        java.util.Map<String, Integer> input = new java.util.HashMap<>();
        input.put("key1", 1);
        input.put("key2", 2);

        Object result = argDef.convertAndValidate(input, null);
        assertNotNull(result);
        assertTrue(result instanceof java.util.Map);
    }

    @Test
    public void testConvertAndValidateWithDefaultValueNull() {
        TypeDefinition typeDef = TypeDefinition.string();
        ArgumentDefinition argDef = new ArgumentDefinition("name", typeDef, "default");

        Object result = argDef.convertAndValidate(null, null);
        assertEquals("default", result);
    }
}

