package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentTypeTest {

    @Test
    public void testFromLuaString() {
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("string"));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("integer"));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("int"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("double"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("number"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("float"));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("boolean"));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("bool"));
    }

    @Test
    public void testFromLuaStringCaseInsensitive() {
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("STRING"));
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("String"));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("INTEGER"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("DOUBLE"));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("BOOLEAN"));
    }

    @Test
    public void testFromLuaStringWithWhitespace() {
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("  string  "));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("  integer  "));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("  double  "));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("  boolean  "));
    }

    @Test
    public void testFromLuaStringNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString(null);
        });
    }

    @Test
    public void testFromLuaStringUnsupportedThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("unsupported");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("array");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("table");
        });
    }

    @Test
    public void testConvertValueString() {
        Object result = ArgumentType.STRING.convertValue("test");
        assertEquals("test", result);

        result = ArgumentType.STRING.convertValue(123);
        assertEquals("123", result);

        result = ArgumentType.STRING.convertValue(true);
        assertEquals("true", result);

        result = ArgumentType.STRING.convertValue(null);
        assertNull(result);
    }

    @Test
    public void testConvertValueInteger() {
        Object result = ArgumentType.INTEGER.convertValue(42);
        assertEquals(42, result);
        assertTrue(result instanceof Integer);

        result = ArgumentType.INTEGER.convertValue(42.5);
        assertEquals(42, result);

        result = ArgumentType.INTEGER.convertValue(42L);
        assertEquals(42, result);

        result = ArgumentType.INTEGER.convertValue("42");
        assertEquals(42, result);

        result = ArgumentType.INTEGER.convertValue(null);
        assertNull(result);
    }

    @Test
    public void testConvertValueIntegerFromString() {
        Object result = ArgumentType.INTEGER.convertValue("123");
        assertEquals(123, result);

        result = ArgumentType.INTEGER.convertValue("-456");
        assertEquals(-456, result);
    }

    @Test
    public void testConvertValueIntegerInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.INTEGER.convertValue("not a number");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.INTEGER.convertValue("abc");
        });
    }

    @Test
    public void testConvertValueDouble() {
        Object result = ArgumentType.DOUBLE.convertValue(42.5);
        assertEquals(42.5, result);
        assertTrue(result instanceof Double);

        result = ArgumentType.DOUBLE.convertValue(42);
        assertEquals(42.0, result);

        result = ArgumentType.DOUBLE.convertValue(42L);
        assertEquals(42.0, result);

        result = ArgumentType.DOUBLE.convertValue("42.5");
        assertEquals(42.5, result);

        result = ArgumentType.DOUBLE.convertValue(null);
        assertNull(result);
    }

    @Test
    public void testConvertValueDoubleFromString() {
        Object result = ArgumentType.DOUBLE.convertValue("123.456");
        assertEquals(123.456, result);

        result = ArgumentType.DOUBLE.convertValue("-42.5");
        assertEquals(-42.5, result);

        result = ArgumentType.DOUBLE.convertValue("0.0");
        assertEquals(0.0, result);
    }

    @Test
    public void testConvertValueDoubleInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.DOUBLE.convertValue("not a number");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.DOUBLE.convertValue("abc");
        });
    }

    @Test
    public void testConvertValueBoolean() {
        Object result = ArgumentType.BOOLEAN.convertValue(true);
        assertEquals(true, result);

        result = ArgumentType.BOOLEAN.convertValue(false);
        assertEquals(false, result);

        result = ArgumentType.BOOLEAN.convertValue("true");
        assertEquals(true, result);

        result = ArgumentType.BOOLEAN.convertValue("false");
        assertEquals(false, result);

        result = ArgumentType.BOOLEAN.convertValue(null);
        assertNull(result);
    }

    @Test
    public void testConvertValueBooleanFromString() {
        assertEquals(true, ArgumentType.BOOLEAN.convertValue("true"));
        assertEquals(false, ArgumentType.BOOLEAN.convertValue("false"));
        assertEquals(true, ArgumentType.BOOLEAN.convertValue("TRUE"));
        assertEquals(false, ArgumentType.BOOLEAN.convertValue("FALSE"));
    }

    @Test
    public void testConvertValueIntegerFromNumberTypes() {
        assertEquals(42, ArgumentType.INTEGER.convertValue((byte) 42));
        assertEquals(42, ArgumentType.INTEGER.convertValue((short) 42));
        assertEquals(42, ArgumentType.INTEGER.convertValue(42));
        assertEquals(42, ArgumentType.INTEGER.convertValue(42L));
        assertEquals(42, ArgumentType.INTEGER.convertValue(42.0f));
        assertEquals(42, ArgumentType.INTEGER.convertValue(42.0));
    }

    @Test
    public void testConvertValueDoubleFromNumberTypes() {
        assertEquals(42.0, ArgumentType.DOUBLE.convertValue((byte) 42));
        assertEquals(42.0, ArgumentType.DOUBLE.convertValue((short) 42));
        assertEquals(42.0, ArgumentType.DOUBLE.convertValue(42));
        assertEquals(42.0, ArgumentType.DOUBLE.convertValue(42L));
        assertEquals(42.0, ArgumentType.DOUBLE.convertValue(42.0f));
        assertEquals(42.0, ArgumentType.DOUBLE.convertValue(42.0));
    }
}

