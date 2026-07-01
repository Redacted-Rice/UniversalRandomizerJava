package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionResultTest {

    @Test
    public void testSuccessfulExecutionStoresResult() {
        LuaValue luaValue = LuaString.valueOf("success");
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 100);

        ExecutionResult execResult = ExecutionResult.success(request, 12445, luaValue);

        assertEquals("TestModule", execResult.getModuleName());
        assertTrue(execResult.isSuccess());
        assertNull(execResult.getErrorMessage());
        assertEquals(luaValue, execResult.getResult());
        assertEquals(12445, execResult.getSeedUsed());
    }

    @Test
    public void testSuccessfulExecutionWithNullResult() {
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 50);
        ExecutionResult execResult = ExecutionResult.success(request, 12395, null);

        assertNull(execResult.getResult());
        assertEquals(12395, execResult.getSeedUsed());
    }

    @Test
    public void testFailedExecutionPopulatesError() {
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 0);
        ExecutionResult execResult = ExecutionResult.scriptFailure(request, "Error message");

        assertEquals("TestModule", execResult.getModuleName());
        assertFalse(execResult.isSuccess());
        assertEquals("Error message", execResult.getErrorMessage());
        assertEquals(LuaValue.NIL, execResult.getResult());
        assertEquals(0, execResult.getSeedUsed());
    }

    @Test
    public void testToStringSuccessful() {
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 10);
        ExecutionResult execResult = ExecutionResult.success(request, 12355, null);

        String text = execResult.toString();
        assertTrue(text.contains("TestModule"));
        assertTrue(text.contains("success=true"));
        assertTrue(text.contains("seed=12355"));
    }

    @Test
    public void testToStringFailed() {
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset("BrokenModule", null, 0);
        ExecutionResult execResult = ExecutionResult.scriptFailure(request, "Test error");

        String text = execResult.toString();
        assertTrue(text.contains("BrokenModule"));
        assertTrue(text.contains("success=false"));
        assertTrue(text.contains("error='Test error'"));
    }

    @Test
    public void testLuaResultValueIsPreserved() {
        LuaInteger luaInt = LuaInteger.valueOf(42);
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 5);
        ExecutionResult execResult = ExecutionResult.success(request, 12350, luaInt);

        assertEquals(luaInt, execResult.getResult());
        assertEquals(42, execResult.getResult().toint());
    }
}
