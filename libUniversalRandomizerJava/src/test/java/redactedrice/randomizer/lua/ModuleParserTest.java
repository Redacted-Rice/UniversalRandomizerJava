package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModuleParserTest {

    @Test
    public void testHashNameToSeedOffsetIsInRange() {
        int offset = ModuleParser.hashNameToSeedOffset("Generated Module");
        assertTrue(offset >= 1 && offset <= 9999);
    }

    @Test
    public void testHashNameToSeedOffsetIsRepeatable() {
        assertEquals(ModuleParser.hashNameToSeedOffset("Module A"),
                ModuleParser.hashNameToSeedOffset("Module A"));
    }

    @Test
    public void testHashNameToSeedOffsetVariesByName() {
        assertNotEquals(ModuleParser.hashNameToSeedOffset("Module A"),
                ModuleParser.hashNameToSeedOffset("Module B"));
    }
}
