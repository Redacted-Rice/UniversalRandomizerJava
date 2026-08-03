package redactedrice.randomizer.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Static collector for warnings and errors.
//
// Logging happens immediately in addWarning/addError via Logger — do not iterate the store
// afterwards to re-log. The store exists so hosts can query failure state (hasErrors) and
// present a popup summary, then clear when the phase is finished.
//
// Snapshot API (internal baselines, no Counts returned to callers):
//   snapshot()                     — remember current totals (stack-aware for nesting)
//   getErrorCountSinceSnapshot()   — errors added since the latest snapshot
//   getWarningCountSinceSnapshot() — warnings added since the latest snapshot
//   clearSnapshot()                — pop the latest snapshot
//   logDeltaSummary(label)         — log a one-line count summary if anything changed
//
// Plain Logger.* never collects. Clear at phase end (after display / after host is done
// inspecting), and again at the start of the next phase if needed.
public final class IssueTracker {
    public enum Severity {
        WARNING, ERROR
    }

    public record Issue(Severity severity, String message, String context) {
        public Issue {
            if (severity == null) {
                throw new IllegalArgumentException("severity cannot be null");
            }
            if (message == null) {
                throw new IllegalArgumentException("message cannot be null");
            }
        }

        public boolean isError() {
            return severity == Severity.ERROR;
        }

        public boolean isWarning() {
            return severity == Severity.WARNING;
        }
    }

    private static final List<Issue> issues = new ArrayList<>();
    private static final Deque<Integer> errorSnapshots = new ArrayDeque<>();
    private static final Deque<Integer> warningSnapshots = new ArrayDeque<>();

    private IssueTracker() {}

    public static void addWarning(String message) {
        addWarning(null, message);
    }

    public static void addWarning(String context, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalized = message.trim();
        synchronized (issues) {
            issues.add(new Issue(Severity.WARNING, normalized, context));
        }
        Logger.warn(formatForLog(context, normalized));
    }

    public static void addError(String message) {
        addError(null, message);
    }

    public static void addError(String context, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalized = message.trim();
        synchronized (issues) {
            issues.add(new Issue(Severity.ERROR, normalized, context));
        }
        Logger.error(formatForLog(context, normalized));
    }

    public static boolean hasErrors() {
        synchronized (issues) {
            return issues.stream().anyMatch(Issue::isError);
        }
    }

    public static boolean hasWarnings() {
        synchronized (issues) {
            return issues.stream().anyMatch(Issue::isWarning);
        }
    }

    public static boolean hasIssues() {
        synchronized (issues) {
            return !issues.isEmpty();
        }
    }

    public static int getErrorCount() {
        synchronized (issues) {
            return countErrorsLocked();
        }
    }

    public static int getWarningCount() {
        synchronized (issues) {
            return countWarningsLocked();
        }
    }

    // Remember current totals so later get*CountSinceSnapshot() reports only new issues.
    // Nested calls push; pair with clearSnapshot() in a finally block.
    public static void snapshot() {
        synchronized (issues) {
            errorSnapshots.push(countErrorsLocked());
            warningSnapshots.push(countWarningsLocked());
        }
    }

    public static int getErrorCountSinceSnapshot() {
        synchronized (issues) {
            int baseline = errorSnapshots.isEmpty() ? 0 : errorSnapshots.peek();
            return Math.max(0, countErrorsLocked() - baseline);
        }
    }

    public static int getWarningCountSinceSnapshot() {
        synchronized (issues) {
            int baseline = warningSnapshots.isEmpty() ? 0 : warningSnapshots.peek();
            return Math.max(0, countWarningsLocked() - baseline);
        }
    }

    public static boolean hasIssuesSinceSnapshot() {
        return getErrorCountSinceSnapshot() > 0 || getWarningCountSinceSnapshot() > 0;
    }

    // Pop the latest snapshot baseline. No-op if none.
    public static void clearSnapshot() {
        synchronized (issues) {
            if (!errorSnapshots.isEmpty()) {
                errorSnapshots.pop();
            }
            if (!warningSnapshots.isEmpty()) {
                warningSnapshots.pop();
            }
        }
    }

    // Logs a one-line count summary for issues added since the latest snapshot. No-op if
    // unchanged. Does not pop the snapshot — call clearSnapshot() when the scope ends.
    public static boolean logDeltaSummary(String label) {
        int errors = getErrorCountSinceSnapshot();
        int warnings = getWarningCountSinceSnapshot();
        if (errors == 0 && warnings == 0) {
            return false;
        }
        String scope = (label == null || label.isBlank()) ? "Phase" : label.trim();
        Logger.info(scope + " finished with " + formatDeltaCounts(errors, warnings) + " (see log)");
        return true;
    }

    public static List<Issue> getIssues() {
        synchronized (issues) {
            return List.copyOf(issues);
        }
    }

    public static List<String> getErrors() {
        synchronized (issues) {
            return issues.stream().filter(Issue::isError).map(Issue::message).toList();
        }
    }

    public static List<String> getWarnings() {
        synchronized (issues) {
            return issues.stream().filter(Issue::isWarning).map(Issue::message).toList();
        }
    }

    public static List<Issue> getIssues(Severity severity) {
        if (severity == null) {
            return List.of();
        }
        synchronized (issues) {
            return issues.stream().filter(i -> i.severity() == severity).toList();
        }
    }

    public static void clear() {
        synchronized (issues) {
            issues.clear();
            errorSnapshots.clear();
            warningSnapshots.clear();
        }
    }

    public static void clearWarnings() {
        synchronized (issues) {
            issues.removeIf(Issue::isWarning);
        }
    }

    public static void clearErrors() {
        synchronized (issues) {
            issues.removeIf(Issue::isError);
        }
    }

    private static int countErrorsLocked() {
        return (int) issues.stream().filter(Issue::isError).count();
    }

    private static int countWarningsLocked() {
        return (int) issues.stream().filter(Issue::isWarning).count();
    }

    private static String formatDeltaCounts(int errors, int warnings) {
        List<String> parts = new ArrayList<>(2);
        if (errors > 0) {
            parts.add(errors + " error(s)");
        }
        if (warnings > 0) {
            parts.add(warnings + " warning(s)");
        }
        return String.join(", ", parts);
    }

    private static String formatForLog(String context, String message) {
        if (context == null || context.isBlank()) {
            return message;
        }
        return context + ": " + message;
    }
}
