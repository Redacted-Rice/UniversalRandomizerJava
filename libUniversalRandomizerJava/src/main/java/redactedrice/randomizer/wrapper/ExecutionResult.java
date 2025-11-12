package redactedrice.randomizer.wrapper;

import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;

// holds the result of running a lua randomizer module
public class ExecutionResult {
    String moduleName;
    boolean success;
    String errorMessage;
    LuaValue result;
    Map<String, Map<String, String>> changes; // objectName -> {field -> change}
    int seedUsed;

    // TODO: Clean this up - think we don't need changes at least now that we are doing it in lua
    public ExecutionResult(String moduleName, boolean success, String errorMessage, LuaValue result,
            Map<String, Map<String, String>> changes, int seedUsed) {
        this.moduleName = moduleName;
        this.success = success;
        this.errorMessage = errorMessage;
        this.result = result;
        this.changes = changes;
        this.seedUsed = seedUsed;
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

    public Map<String, Map<String, String>> getChanges() {
        // return a copy so external modifications dont affect internal state
        return changes != null ? new HashMap<>(changes) : null;
    }

    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    public int getSeedUsed() {
        return seedUsed;
    }

    public String getChangesFormatted() {
        if (changes == null || changes.isEmpty()) {
            return "No changes detected";
        }

        // build a string representation of all changes
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> objEntry : changes.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(objEntry.getKey()).append(": ");

            boolean first = true;
            for (Map.Entry<String, String> fieldChange : objEntry.getValue().entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(fieldChange.getKey()).append(": ").append(fieldChange.getValue());
                first = false;
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (success) {
            String changesStr = hasChanges() ? ", changes={" + getChangesFormatted() + "}" : "";
            return "ExecutionResult{module='" + moduleName + "', success=true, seed=" + seedUsed
                    + changesStr + "}";
        } else {
            return "ExecutionResult{module='" + moduleName + "', success=false, error='"
                    + errorMessage + "'}";
        }
    }
}

