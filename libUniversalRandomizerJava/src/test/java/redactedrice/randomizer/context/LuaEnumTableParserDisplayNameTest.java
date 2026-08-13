package redactedrice.randomizer.context;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LuaEnumTableParserDisplayNameTest {

    @Test
    public void testParseDisplayNamesFromRegistrationTable() {
        LuaTable table = new LuaTable();
        table.set(1, LuaValue.valueOf("FIRE"));
        table.set(2, LuaValue.valueOf("WATER"));
        table.set("values", buildValuesTable(Map.of("FIRE", 0, "WATER", 1)));

        LuaTable displayNames = new LuaTable();
        displayNames.set("FIRE", LuaValue.valueOf("Fire"));
        displayNames.set("WATER", LuaValue.valueOf("Water"));
        table.set("displayNames", displayNames);

        ParsedEnumData parsed = LuaEnumTableParser.parseEnumTable("EnergyType", table);

        assertEquals(java.util.Arrays.asList("FIRE", "WATER"), parsed.getValueNames());
        assertEquals("Fire", parsed.getValueDisplayNames().get("FIRE"));
        assertEquals("Water", parsed.getValueDisplayNames().get("WATER"));
    }

    private static LuaTable buildValuesTable(Map<String, Integer> values) {
        LuaTable table = new LuaTable();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            table.set(entry.getKey(), LuaValue.valueOf(entry.getValue()));
        }
        return table;
    }
}
