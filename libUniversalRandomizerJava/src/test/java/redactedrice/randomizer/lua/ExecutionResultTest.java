package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionResultTest {

    @Test
    void successCapturesResultAndSeed() {
        LuaValue luaValue = LuaString.valueOf("success");
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 100);
        ExecutionResult execResult = ExecutionResult.success(request, 12445, luaValue);

        assertEquals("TestModule", execResult.getModuleId());
        assertTrue(execResult.isSuccess());
        assertNull(execResult.getErrorMessage());
        assertEquals(luaValue, execResult.getResult());
        assertEquals(12445, execResult.getSeedUsed());

        ExecutionRequest nullResultRequest =
                ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 50);
        ExecutionResult nullResult =
                ExecutionResult.success(nullResultRequest, 12395, null);
        assertNull(nullResult.getResult());
        assertEquals(12395, nullResult.getSeedUsed());
    }

    @Test
    void failureCapturesErrorMessage() {
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 0);
        ExecutionResult execResult = ExecutionResult.scriptFailure(request, "Error message");

        assertEquals("TestModule", execResult.getModuleId());
        assertFalse(execResult.isSuccess());
        assertEquals("Error message", execResult.getErrorMessage());
        assertEquals(LuaValue.NIL, execResult.getResult());
        assertEquals(0, execResult.getSeedUsed());
    }

    @Test
    void toStringReflectsOutcome() {
        ExecutionRequest successRequest =
                ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 10);
        ExecutionResult success = ExecutionResult.success(successRequest, 12355, null);
        String successText = success.toString();
        assertTrue(successText.contains("TestModule"));
        assertTrue(successText.contains("success=true"));
        assertTrue(successText.contains("seed=12355"));

        ExecutionRequest failureRequest =
                ExecutionRequest.forModuleWithSeedOffset("BrokenModule", null, 0);
        ExecutionResult failure = ExecutionResult.scriptFailure(failureRequest, "Test error");
        String failureText = failure.toString();
        assertTrue(failureText.contains("BrokenModule"));
        assertTrue(failureText.contains("success=false"));
        assertTrue(failureText.contains("error='Test error'"));
    }

    @Test
    void luaResultValueIsPreserved() {
        LuaInteger luaInt = LuaInteger.valueOf(42);
        ExecutionRequest request = ExecutionRequest.forModuleWithSeedOffset("TestModule", null, 5);
        ExecutionResult execResult = ExecutionResult.success(request, 12350, luaInt);

        assertEquals(luaInt, execResult.getResult());
        assertEquals(42, execResult.getResult().toint());
    }
}
