package redactedrice.randomizer.utils;

import java.util.ArrayList;
import java.util.List;

// Static collector for warnings and errors.
//
// Logging happens immediately in addWarning/addError via Logger - do not iterate the store
// afterwards to re-log. The store exists so hosts can query failure state (hasErrors) and
// present a popup summary, then clear when the phase is finished.
//
// Snapshot API (single baseline pair - call sites are sequential, not nested):
// snapshot() - remember current totals
// getErrorCountSinceSnapshot() - errors added since the snapshot (or all, if none)
// getWarningCountSinceSnapshot() - warnings added since the snapshot (or all, if none)
// clearSnapshot() - reset baselines to 0
// logDeltaSummary(label) - log a one-line count summary if anything changed
// clearErrors()/clearWarnings() - also reset matching snapshot baselines to 0
//
// Plain Logger.warn/error collect into IssueTracker when Logger collection is enabled (default on).
// IssueTracker.addWarning/addError always collect and log once via Logger.log. A "phase" is one
// host batch (e.g. loadModules or one executeModules / randomize run), not each module inside it.
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
    private static int errorSnapshotBaseline = 0;
    private static int warningSnapshotBaseline = 0;

    private IssueTracker() {}

    public static void addWarning(String message) {
        addWarning(null, message);
    }

    public static void addWarning(String context, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalized = message.trim();
        recordWarning(context, normalized);
        Logger.log(LogLevel.WARN, formatForLog(context, normalized));
    }

    public static void addError(String message) {
        addError(null, message);
    }

    public static void addError(String context, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalized = message.trim();
        recordError(context, normalized);
        Logger.log(LogLevel.ERROR, formatForLog(context, normalized));
    }

    /** Store a warning without logging. Used by {@link Logger#warn} to avoid double collection. */
    static void recordWarning(String context, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalized = message.trim();
        synchronized (issues) {
            issues.add(new Issue(Severity.WARNING, normalized, context));
        }
    }

    /** Store an error without logging. Used by {@link Logger#error} to avoid double collection. */
    static void recordError(String context, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalized = message.trim();
        synchronized (issues) {
            issues.add(new Issue(Severity.ERROR, normalized, context));
        }
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

    // Remember current totals so later get*CountSinceSnapshot reports only new issues.
    // Pair with clearSnapshot in a finally block. A second snapshot overwrites.
    public static void snapshot() {
        synchronized (issues) {
            errorSnapshotBaseline = countErrorsLocked();
            warningSnapshotBaseline = countWarningsLocked();
        }
    }

    public static int getErrorCountSinceSnapshot() {
        synchronized (issues) {
            return Math.max(0, countErrorsLocked() - errorSnapshotBaseline);
        }
    }

    public static int getWarningCountSinceSnapshot() {
        synchronized (issues) {
            return Math.max(0, countWarningsLocked() - warningSnapshotBaseline);
        }
    }

    public static boolean hasIssuesSinceSnapshot() {
        return getErrorCountSinceSnapshot() > 0 || getWarningCountSinceSnapshot() > 0;
    }

    // Reset baselines to 0 (deltas then count from an empty prior state).
    public static void clearSnapshot() {
        synchronized (issues) {
            errorSnapshotBaseline = 0;
            warningSnapshotBaseline = 0;
        }
    }

    // Logs a one line count summary for issues added since the snapshot. No op if
    // unchanged. Does not clear the snapshot - call clearSnapshot when the scope ends.
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
            errorSnapshotBaseline = 0;
            warningSnapshotBaseline = 0;
        }
    }

    public static void clearWarnings() {
        synchronized (issues) {
            issues.removeIf(Issue::isWarning);
            warningSnapshotBaseline = 0;
        }
    }

    public static void clearErrors() {
        synchronized (issues) {
            issues.removeIf(Issue::isError);
            errorSnapshotBaseline = 0;
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
