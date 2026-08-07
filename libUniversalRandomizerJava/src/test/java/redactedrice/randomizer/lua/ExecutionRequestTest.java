package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionRequestTest {
    private Module createModule(String id, int seedOffset, boolean fromMetadata, boolean seeded) {
        LuaFunction executeFunc = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        return new Module(id, "Offset Module", null, null, null, null, executeFunc, null, null,
                seedOffset, fromMetadata, seeded, "module", "Author", "0.1", Map.of(), null, null, null, null, null);
    }

    @Test
    void forModuleUsesModuleSeedOffset() {
        Module module = createModule("offset_module", 77, true, true);
        ExecutionRequest request = ExecutionRequest.forModule(module, Map.of());

        assertEquals(77, request.getSeedOffset());
        assertFalse(request.hasExplicitSeedOffset());
        assertEquals(12422, request.resolveAbsoluteSeed(12345));
    }

    @Test
    void forModuleWithSeedOffsetOverridesModuleDefault() {
        Module module = createModule("offset_module", 77, true, true);
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset(module, Map.of(), 99);

        assertEquals(99, request.getSeedOffset());
        assertTrue(request.hasExplicitSeedOffset());
        assertEquals(12444, request.resolveAbsoluteSeed(12345));
    }

    @Test
    void unseededModuleSkipsSeedOffset() {
        Module module = createModule("unseeded_module", 77, true, false);
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset(module, Map.of(), 99);

        assertFalse(request.usesSeed());
        assertEquals(0, request.getSeedOffset());
        assertThrows(IllegalStateException.class, () -> request.resolveAbsoluteSeed(12345));

        Module unseeded = createModule("unseeded_module", 0, false, false);
        ExecutionRequest factoryRequest = ExecutionRequest.forUnseededModule(unseeded, Map.of());
        assertFalse(factoryRequest.usesSeed());
        assertFalse(factoryRequest.hasExplicitSeedOffset());
    }
}
