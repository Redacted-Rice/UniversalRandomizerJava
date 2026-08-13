package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentParserTest {

    @Test
    public void testParseArgumentDisplayName() {
        LuaTable moduleTable = new LuaTable();
        LuaTable argsTable = new LuaTable();
        LuaTable argTable = new LuaTable();
        argTable.set("name", LuaValue.valueOf("grouping"));
        argTable.set("displayName", LuaValue.valueOf("Stage grouping"));
        argTable.set("definition", LuaValue.valueOf("string"));
        argTable.set("default", LuaValue.valueOf("BY_STAGE"));
        argsTable.set(1, argTable);
        moduleTable.set("arguments", argsTable);

        List<ArgumentDefinition> arguments =
                ArgumentParser.parseArgumentsFromTable(moduleTable, "test.lua");
        assertNotNull(arguments);
        assertEquals(1, arguments.size());
        assertEquals("grouping", arguments.get(0).getName());
        assertEquals("Stage grouping", arguments.get(0).getDisplayName());
    }
}
