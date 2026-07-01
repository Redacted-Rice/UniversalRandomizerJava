package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionRequestTest {
    private Module createModule(String name, int seedOffset, boolean fromMetadata) {
        LuaFunction executeFunc = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        return new Module(name, null, null, null, null, executeFunc, null, null, seedOffset,
                fromMetadata, "module", "Author", "0.1", Map.of("UniversalRandomizerJava", "0.5.0"),
                null, null, null);
    }

    @Test
    public void testForModuleUsesModuleSeedOffset() {
        Module module = createModule("Offset Module", 77, true);
        ExecutionRequest request = ExecutionRequest.forModule(module, Map.of());

        assertEquals(77, request.getSeedOffset());
        assertFalse(request.hasExplicitSeedOffset());
        assertEquals(12422, request.resolveAbsoluteSeed(12345));
    }

    @Test
    public void testForModuleWithSeedOffsetOverridesModuleDefault() {
        Module module = createModule("Offset Module", 77, true);
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset(module, Map.of(), 99);

        assertEquals(99, request.getSeedOffset());
        assertTrue(request.hasExplicitSeedOffset());
        assertEquals(12444, request.resolveAbsoluteSeed(12345));
    }
}
