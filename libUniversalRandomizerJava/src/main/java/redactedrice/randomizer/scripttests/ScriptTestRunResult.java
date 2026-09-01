package redactedrice.randomizer.scripttests;

import java.util.List;

// Outcome of running one or more case files. Hosts use this instead of parsing CLI output.
public record ScriptTestRunResult(int passed, int failed, List<ScriptTestFailure> failures) {
    public boolean isSuccess() {
        return failed == 0;
    }

    public int exitCode() {
        return isSuccess() ? 0 : 1;
    }
}
