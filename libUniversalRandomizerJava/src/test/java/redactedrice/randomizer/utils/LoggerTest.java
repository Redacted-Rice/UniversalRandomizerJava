package redactedrice.randomizer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redactedrice.randomizer.utils.IssueTracker;

class LoggerTest {
    @BeforeEach
    void setUp() {
        Logger.reset();
        IssueTracker.clear();
    }

    @AfterEach
    void tearDown() {
        Logger.reset();
        IssueTracker.clear();
    }

    @Test
    void debugIsFilteredWhenMinLevelIsInfo() {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        Logger.addStreamForAllLevels(capture);
        Logger.setMinLogLevel(LogLevel.INFO);
        Logger.setFormatString("%MESSAGE");

        Logger.debug("debug-only-message");
        Logger.info("info-only-message");

        String output = capture.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("debug-only-message"));
        assertTrue(output.contains("info-only-message"));
    }

    @Test
    void debugIsEmittedWhenMinLevelIsDebug() {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        Logger.addStreamForAllLevels(capture);
        Logger.setMinLogLevel(LogLevel.DEBUG);
        Logger.setFormatString("%MESSAGE");

        Logger.debug("debug-visible-message");

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("debug-visible-message"));
    }

    @Test
    void warnCollectsToIssueTrackerByDefault() {
        Logger.setFormatString("%MESSAGE");
        Logger.warn("popup warning");

        assertTrue(IssueTracker.hasWarnings());
        assertEquals(List.of("popup warning"), IssueTracker.getWarnings());
    }

    @Test
    void warnDoesNotCollectWhenDisabled() {
        Logger.setCollectWarningsToIssueTracker(false);
        Logger.warn("log only");

        assertFalse(IssueTracker.hasWarnings());
    }

    @Test
    void addWarningDoesNotDoubleCollectWhenLoggerCollectionEnabled() {
        IssueTracker.addWarning("via tracker");
        assertEquals(1, IssueTracker.getWarningCount());
        assertEquals(List.of("via tracker"), IssueTracker.getWarnings());
    }
}
