package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;

// Tests for ExecutionResult class
// verifies factory helpers and basic value propagation
public class ExecutionResultTest {

    @Test
    public void testSuccessfulExecutionStoresResult() {
        LuaValue luaValue = LuaString.valueOf("success");
        ExecutionRequest request = ExecutionRequest.withSeed("TestModule", null, 12345);

        ExecutionResult execResult = ExecutionResult.success(request, luaValue);

        assertEquals("TestModule", execResult.getModuleName());
        assertTrue(execResult.isSuccess());
        assertNull(execResult.getErrorMessage());
        assertEquals(luaValue, execResult.getResult());
        assertEquals(12345, execResult.getSeedUsed());
    }

    @Test
    public void testSuccessfulExecutionWithNullResult() {
        ExecutionRequest request = ExecutionRequest.withSeed("TestModule", null, 9876);
        ExecutionResult execResult = ExecutionResult.success(request, null);

        assertNull(execResult.getResult());
        assertEquals(9876, execResult.getSeedUsed());
    }

    @Test
    public void testFailedExecutionPopulatesError() {
        ExecutionResult execResult = ExecutionResult.scriptFailure("TestModule", "Error message");

        assertEquals("TestModule", execResult.getModuleName());
        assertFalse(execResult.isSuccess());
        assertEquals("Error message", execResult.getErrorMessage());
        assertEquals(LuaValue.NIL, execResult.getResult());
        assertEquals(0, execResult.getSeedUsed());
    }

    @Test
    public void testToStringSuccessful() {
        ExecutionRequest request = ExecutionRequest.withSeed("TestModule", null, 555);
        ExecutionResult execResult = ExecutionResult.success(request, null);

        String text = execResult.toString();
        assertTrue(text.contains("TestModule"));
        assertTrue(text.contains("success=true"));
        assertTrue(text.contains("seed=555"));
    }

    @Test
    public void testToStringFailed() {
        ExecutionResult execResult = ExecutionResult.scriptFailure("BrokenModule", "Test error");

        String text = execResult.toString();
        assertTrue(text.contains("BrokenModule"));
        assertTrue(text.contains("success=false"));
        assertTrue(text.contains("error='Test error'"));
    }

    @Test
    public void testLuaResultValueIsPreserved() {
        LuaInteger luaInt = LuaInteger.valueOf(42);
        ExecutionRequest request = ExecutionRequest.withSeed("TestModule", null, 999);
        ExecutionResult execResult = ExecutionResult.success(request, luaInt);

        assertEquals(luaInt, execResult.getResult());
        assertEquals(42, execResult.getResult().toint());
    }
}

