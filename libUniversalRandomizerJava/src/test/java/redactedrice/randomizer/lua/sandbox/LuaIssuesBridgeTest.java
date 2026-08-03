package redactedrice.randomizer.lua.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redactedrice.randomizer.utils.IssueTracker;

class LuaIssuesBridgeTest {
    @TempDir
    Path tempDir;

    private LuaSandbox sandbox;

    @BeforeEach
    void setUp() {
        IssueTracker.clear();
        sandbox = new LuaSandbox(List.of(tempDir.toAbsolutePath().toString()));
    }

    @AfterEach
    void tearDown() {
        IssueTracker.clear();
    }

    @Test
    void issuesWarnAndErrorCollectOnTracker() {
        sandbox.execute("""
                logger.warn("stream only")
                issues.warn("tracked warning")
                issues.error("tracked error")
                """);

        assertFalse(IssueTracker.getWarnings().contains("stream only"));
        assertEquals(List.of("tracked warning"), IssueTracker.getWarnings());
        assertEquals(List.of("tracked error"), IssueTracker.getErrors());
        assertTrue(IssueTracker.hasErrors());
    }

    @Test
    void loggerWarnDoesNotCollect() {
        sandbox.execute("logger.warn('not collected')");
        assertFalse(IssueTracker.hasWarnings());
        assertFalse(IssueTracker.hasErrors());
    }
}
