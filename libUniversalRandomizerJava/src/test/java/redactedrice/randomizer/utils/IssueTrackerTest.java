package redactedrice.randomizer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IssueTrackerTest {
    @BeforeEach
    void setUp() {
        IssueTracker.clear();
    }

    @AfterEach
    void tearDown() {
        IssueTracker.clear();
    }

    @Test
    void addWarningAndErrorAreCollectedSeparately() {
        IssueTracker.addWarning("soft problem");
        IssueTracker.addError("hard problem");

        assertTrue(IssueTracker.hasWarnings());
        assertTrue(IssueTracker.hasErrors());
        assertEquals(1, IssueTracker.getWarningCount());
        assertEquals(1, IssueTracker.getErrorCount());
        assertEquals(List.of("soft problem"), IssueTracker.getWarnings());
        assertEquals(List.of("hard problem"), IssueTracker.getErrors());
    }

    @Test
    void clearWarningsLeavesErrors() {
        IssueTracker.addWarning("w");
        IssueTracker.addError("e");
        IssueTracker.clearWarnings();

        assertFalse(IssueTracker.hasWarnings());
        assertTrue(IssueTracker.hasErrors());
        assertEquals(List.of("e"), IssueTracker.getErrors());
    }

    @Test
    void contextIsStoredOnIssue() {
        IssueTracker.addWarning("module requirements", "version low");

        assertEquals("module requirements", IssueTracker.getIssues().get(0).context());
        assertEquals("version low", IssueTracker.getIssues().get(0).message());
    }

    @Test
    void snapshotDeltaOnlyCountsIssuesSinceSnapshot() {
        IssueTracker.addError("prior error");
        IssueTracker.addWarning("prior warning");
        IssueTracker.snapshot();

        assertEquals(0, IssueTracker.getErrorCountSinceSnapshot());
        assertEquals(0, IssueTracker.getWarningCountSinceSnapshot());
        assertFalse(IssueTracker.hasIssuesSinceSnapshot());

        IssueTracker.addWarning("new warning");
        IssueTracker.addError("new error");

        assertEquals(1, IssueTracker.getErrorCountSinceSnapshot());
        assertEquals(1, IssueTracker.getWarningCountSinceSnapshot());
        assertTrue(IssueTracker.hasIssuesSinceSnapshot());
        // Totals still include prior issues
        assertEquals(2, IssueTracker.getErrorCount());
        assertEquals(2, IssueTracker.getWarningCount());

        IssueTracker.clearSnapshot();
    }

    @Test
    void snapshotOverwritesPreviousBaseline() {
        IssueTracker.addError("prior");
        IssueTracker.snapshot();
        IssueTracker.addError("between");
        IssueTracker.snapshot(); // overwrite
        IssueTracker.addError("after");

        assertEquals(1, IssueTracker.getErrorCountSinceSnapshot());
        IssueTracker.clearSnapshot();
    }

    @Test
    void logDeltaSummaryNoopsWhenUnchanged() {
        IssueTracker.snapshot();
        assertFalse(IssueTracker.logDeltaSummary("Module 'x'"));
        IssueTracker.addWarning("w");
        assertTrue(IssueTracker.logDeltaSummary("Module 'x'"));
        IssueTracker.clearSnapshot();
    }

    @Test
    void clearErrorsResetsActiveErrorSnapshotBaseline() {
        IssueTracker.addError("prior");
        IssueTracker.snapshot();
        IssueTracker.addError("during");
        assertEquals(1, IssueTracker.getErrorCountSinceSnapshot());

        IssueTracker.clearErrors();
        assertEquals(0, IssueTracker.getErrorCount());
        assertEquals(0, IssueTracker.getErrorCountSinceSnapshot());

        IssueTracker.addError("after clear");
        assertEquals(1, IssueTracker.getErrorCountSinceSnapshot());
        IssueTracker.clearSnapshot();
    }

    @Test
    void clearWarningsResetsActiveWarningSnapshotBaseline() {
        IssueTracker.addWarning("prior");
        IssueTracker.snapshot();
        IssueTracker.addWarning("during");
        assertEquals(1, IssueTracker.getWarningCountSinceSnapshot());

        IssueTracker.clearWarnings();
        assertEquals(0, IssueTracker.getWarningCount());
        assertEquals(0, IssueTracker.getWarningCountSinceSnapshot());

        IssueTracker.addWarning("after clear");
        assertEquals(1, IssueTracker.getWarningCountSinceSnapshot());
        IssueTracker.clearSnapshot();
    }
}
