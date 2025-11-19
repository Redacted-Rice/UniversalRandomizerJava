package redactedrice.randomizer.wrapper;

import org.luaj.vm2.LuaValue;
import java.util.Map;

// holds the result of running a lua randomizer module
public class ExecutionResult {
    private final String moduleName;
    // Techincally redundant currently with error message but keeping for clarity
    private final boolean success;
    private final String errorMessage;
    private final LuaValue result;
    // TODO: How should we handle this for scripts? Right now it will be null
    private final ExecutionRequest request;

    protected ExecutionResult(ExecutionRequest request, String moduleName, boolean success,
            LuaValue result, String errorMessage) {
        this.request = request;
        this.moduleName = moduleName;
        this.success = success;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public static ExecutionResult scriptSuccess(String moduleName, LuaValue result) {
        return new ExecutionResult(null, moduleName, true, result, null);
    }

    public static ExecutionResult success(ExecutionRequest request, LuaValue result) {
        return new ExecutionResult(request, request.getModuleName(), true, result, null);
    }

    public static ExecutionResult scriptFailure(String moduleName, String errorMessage) {
        return new ExecutionResult(null, moduleName, false, LuaValue.NIL, errorMessage);
    }

    public static ExecutionResult failure(ExecutionRequest request, String errorMessage) {
        return new ExecutionResult(request, request.getModuleName(), false, LuaValue.NIL,
                errorMessage);
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
        return request != null ? request.getSeed() : 0;
    }

    public ExecutionRequest getRequest() {
        return request;
    }

    public Map<String, Object> getArguments() {
        return request != null ? request.getArguments() : Map.of();
    }

    @Override
    public String toString() {
        if (success) {
            int seed = getSeedUsed();
            return "ExecutionResult{module='" + moduleName + "', success=true, seed=" + seed + "}";
        } else {
            return "ExecutionResult{module='" + moduleName + "', success=false, error='"
                    + errorMessage + "'}";
        }
    }
}

