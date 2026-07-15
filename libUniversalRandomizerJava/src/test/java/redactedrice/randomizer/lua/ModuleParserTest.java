package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModuleParserTest {

    @Test
    public void testHashIdToSeedOffsetIsInRange() {
        int offset = ModuleParser.hashIdToSeedOffset("generated_module");
        assertTrue(offset >= 1 && offset <= 9999);
    }

    @Test
    public void testHashIdToSeedOffsetIsRepeatable() {
        assertEquals(ModuleParser.hashIdToSeedOffset("module_a"),
                ModuleParser.hashIdToSeedOffset("module_a"));
    }

    @Test
    public void testHashIdToSeedOffsetVariesById() {
        assertNotEquals(ModuleParser.hashIdToSeedOffset("module_a"),
                ModuleParser.hashIdToSeedOffset("module_b"));
    }
}
