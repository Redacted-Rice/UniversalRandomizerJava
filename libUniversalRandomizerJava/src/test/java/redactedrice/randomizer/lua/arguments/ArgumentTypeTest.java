package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentTypeTest {

    @Test
    public void testFromLuaStringCaseInsensitive() {
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("STRING"));
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("String"));
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString("string"));

        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("INTEGER"));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("Integer"));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("integer"));

        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("DOUBLE"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("Double"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("double"));

        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("BOOLEAN"));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("Boolean"));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("boolean"));
    }

    @Test
    public void testFromLuaStringAliases() {
        // Integer aliases
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("int"));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString("INT"));

        // Double aliases
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("number"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("Number"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("NUMBER"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("float"));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString("Float"));

        // Boolean alias
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("bool"));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString("Bool"));
    }

    @Test
    public void testFromLuaStringWithWhitespace() {
        assertEquals(ArgumentType.STRING, ArgumentType.fromLuaString(" string "));
        assertEquals(ArgumentType.INTEGER, ArgumentType.fromLuaString(" int "));
        assertEquals(ArgumentType.DOUBLE, ArgumentType.fromLuaString(" number "));
        assertEquals(ArgumentType.BOOLEAN, ArgumentType.fromLuaString(" boolean "));
    }

    @Test
    public void testFromLuaStringNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString(null);
        });
    }

    @Test
    public void testFromLuaStringUnsupportedTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("table");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("function");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("unsupported");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArgumentType.fromLuaString("");
        });
    }
}
