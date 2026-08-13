package redactedrice.randomizer.utils;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import redactedrice.randomizer.context.EnumDefinition;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LuaJavaConverterEnumTableTest {

    @Test
    public void testDisplayNamesMetadataNotOverwrittenByValueAlias() {
        EnumDefinition def = new EnumDefinition("WeirdEnum",
                Arrays.asList("FIRE", "displayNames"), Map.of("FIRE", 0, "displayNames", 1), null,
                Map.of("FIRE", "Fire"));

        LuaTable table = LuaJavaConverter.enumDefinitionToLuaTable("WeirdEnum", def);

        // named alias is reserved for metadata, not the enum value
        assertTrue(table.get("displayNames").istable());
        assertEquals("Fire", table.get("displayNames").get("FIRE").tojstring());
        // value still present in the array part
        assertEquals("FIRE", table.get(1).tojstring());
        assertEquals("displayNames", table.get(2).tojstring());
    }
}
