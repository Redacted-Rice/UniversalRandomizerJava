package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionRequestTest {
    private Module createModule(String id, int seedOffset, boolean fromMetadata) {
        return createModule(id, seedOffset, fromMetadata, true);
    }

    private Module createModule(String id, int seedOffset, boolean fromMetadata,
            boolean seeded) {
        LuaFunction executeFunc = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        return new Module(id, "Offset Module", null, null, null, null, executeFunc, null, null,
                seedOffset, fromMetadata, seeded, "module", "Author", "0.1", Map.of(), null, null,
                null);
    }

    @Test
    public void testForModuleUsesModuleSeedOffset() {
        Module module = createModule("offset_module", 77, true);
        ExecutionRequest request = ExecutionRequest.forModule(module, Map.of());

        assertEquals(77, request.getSeedOffset());
        assertFalse(request.hasExplicitSeedOffset());
        assertEquals(12422, request.resolveAbsoluteSeed(12345));
    }

    @Test
    public void testForModuleWithSeedOffsetOverridesModuleDefault() {
        Module module = createModule("offset_module", 77, true);
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset(module, Map.of(), 99);

        assertEquals(99, request.getSeedOffset());
        assertTrue(request.hasExplicitSeedOffset());
        assertEquals(12444, request.resolveAbsoluteSeed(12345));
    }

    @Test
    public void testUnseededModuleSkipsSeedOffset() {
        Module module = createModule("unseeded_module", 77, true, false);
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset(module, Map.of(), 99);

        assertFalse(request.usesSeed());
        assertEquals(0, request.getSeedOffset());
        assertThrows(IllegalStateException.class, () -> request.resolveAbsoluteSeed(12345));
    }

    @Test
    public void testForUnseededModuleFactory() {
        Module module = createModule("unseeded_module", 0, false, false);
        ExecutionRequest request = ExecutionRequest.forUnseededModule(module, Map.of());

        assertFalse(request.usesSeed());
        assertFalse(request.hasExplicitSeedOffset());
    }
}
