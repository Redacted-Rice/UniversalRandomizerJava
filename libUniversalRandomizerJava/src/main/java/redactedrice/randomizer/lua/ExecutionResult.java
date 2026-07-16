package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaValue;
import java.util.Map;
import java.util.Objects;

// holds the result of running a lua randomizer module
public final class ExecutionResult {
    private final String moduleId;
    // Techincally redundant currently with error message but keeping for clarity
    private final boolean success;
    private final String errorMessage;
    private final LuaValue result;
    private final ExecutionRequest request;
    private final int seedUsed;

    private ExecutionResult(ExecutionRequest request, String moduleId, boolean success,
            LuaValue result, String errorMessage, int seedUsed) {
        this.request = request;
        this.moduleId = moduleId;
        this.success = success;
        this.result = result;
        this.errorMessage = errorMessage;
        this.seedUsed = seedUsed;
    }

    // Pre/post scripts intentionally omit seed tracking
    public static ExecutionResult scriptSuccess(ExecutionRequest request, LuaValue result) {
        return new ExecutionResult(request, request.getModuleId(), true, result, null, 0);
    }

    public static ExecutionResult success(ExecutionRequest request, int seedUsed, LuaValue result) {
        return new ExecutionResult(request, request.getModuleId(), true, result, null, seedUsed);
    }

    public static ExecutionResult scriptFailure(ExecutionRequest request, String errorMessage) {
        return new ExecutionResult(request, request.getModuleId(), false, LuaValue.NIL,
                errorMessage, 0);
    }

    public static ExecutionResult failure(ExecutionRequest request, int seedUsed,
            String errorMessage) {
        return new ExecutionResult(request, request.getModuleId(), false, LuaValue.NIL,
                errorMessage, seedUsed);
    }

    public String getModuleId() {
        return moduleId;
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

    public ExecutionRequest getRequest() {
        return request;
    }

    public Map<String, Object> getArguments() {
        return request != null ? request.getArguments() : Map.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutionResult that = (ExecutionResult) o;
        return success == that.success && seedUsed == that.seedUsed
                && Objects.equals(moduleId, that.moduleId)
                && Objects.equals(errorMessage, that.errorMessage)
                && Objects.equals(result, that.result) && Objects.equals(request, that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleId, success, errorMessage, result, request, seedUsed);
    }

    @Override
    public String toString() {
        if (success) {
            return "ExecutionResult{module='" + moduleId + "', success=true, seed=" + seedUsed
                    + "}";
        } else {
            return "ExecutionResult{module='" + moduleId + "', success=false, error='"
                    + errorMessage + "'}";
        }
    }
}
