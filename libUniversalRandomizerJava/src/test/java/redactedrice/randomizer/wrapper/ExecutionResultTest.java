package redactedrice.randomizer.wrapper;

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

        ExecutionResult execResult = ExecutionResult.success("TestModule", luaValue, 12345);

        assertEquals("TestModule", execResult.getModuleName());
        assertTrue(execResult.isSuccess());
        assertNull(execResult.getErrorMessage());
        assertEquals(luaValue, execResult.getResult());
        assertEquals(12345, execResult.getSeedUsed());
    }

    @Test
    public void testSuccessfulExecutionWithNullResult() {
        ExecutionResult execResult = ExecutionResult.success("TestModule", null, 9876);

        assertNull(execResult.getResult());
        assertEquals(9876, execResult.getSeedUsed());
    }

    @Test
    public void testFailedExecutionPopulatesError() {
        ExecutionResult execResult = ExecutionResult.failure("TestModule", "Error message");

        assertEquals("TestModule", execResult.getModuleName());
        assertFalse(execResult.isSuccess());
        assertEquals("Error message", execResult.getErrorMessage());
        assertEquals(LuaValue.NIL, execResult.getResult());
        assertEquals(0, execResult.getSeedUsed());
    }

    @Test
    public void testToStringSuccessful() {
        ExecutionResult execResult = ExecutionResult.success("TestModule", null, 555);

        String text = execResult.toString();
        assertTrue(text.contains("TestModule"));
        assertTrue(text.contains("success=true"));
        assertTrue(text.contains("seed=555"));
    }

    @Test
    public void testToStringFailed() {
        ExecutionResult execResult = ExecutionResult.failure("BrokenModule", "Test error");

        String text = execResult.toString();
        assertTrue(text.contains("BrokenModule"));
        assertTrue(text.contains("success=false"));
        assertTrue(text.contains("error='Test error'"));
    }

    @Test
    public void testLuaResultValueIsPreserved() {
        LuaInteger luaInt = LuaInteger.valueOf(42);
        ExecutionResult execResult = ExecutionResult.success("TestModule", luaInt, 999);

        assertEquals(luaInt, execResult.getResult());
        assertEquals(42, execResult.getResult().toint());
    }
}

