package redactedrice.randomizer.wrapper;

import org.luaj.vm2.LuaValue;

// holds the result of running a lua randomizer module
public class ExecutionResult {
    private final String moduleName;
    // Techincally redundant currently with error message but keeping for clarity
    private final boolean success;
    private final String errorMessage;
    private final LuaValue result;
    private final int seedUsed;

    protected ExecutionResult(String moduleName, boolean success, String errorMessage,
            LuaValue result, int seedUsed) {
        this.moduleName = moduleName;
        this.success = success;
        this.errorMessage = errorMessage;
        this.result = result;
        this.seedUsed = seedUsed;
    }

    public static ExecutionResult success(String moduleName, LuaValue result, int seedUsed) {
        return new ExecutionResult(moduleName, true, null, result, seedUsed);
    }

    public static ExecutionResult failure(String moduleName, String errorMessage) {
        return new ExecutionResult(moduleName, false, errorMessage, LuaValue.NIL, 0);
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LuaValue getResult() {
        return result;
    }

    public int getSeedUsed() {
        return seedUsed;
    }

    @Override
    public String toString() {
        if (success) {
            return "ExecutionResult{module='" + moduleName + "', success=true, seed=" + seedUsed
                    + "}";
        } else {
            return "ExecutionResult{module='" + moduleName + "', success=false, error='"
                    + errorMessage + "'}";
        }
    }
}

