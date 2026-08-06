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
import redactedrice.randomizer.utils.Logger;

class LuaLoggerBridgeTest {
    @TempDir
    Path tempDir;

    private LuaSandbox sandbox;

    @BeforeEach
    void setUp() {
        IssueTracker.clear();
        Logger.reset();
        sandbox = new LuaSandbox(List.of(tempDir.toAbsolutePath().toString()));
    }

    @AfterEach
    void tearDown() {
        IssueTracker.clear();
        Logger.reset();
    }

    @Test
    void loggerWarnCollectsByDefault() {
        sandbox.execute("logger.warn('via logger')");
        assertEquals(List.of("via logger"), IssueTracker.getWarnings());
    }

    @Test
    void loggerErrorCollectsByDefault() {
        sandbox.execute("logger.error('via logger')");
        assertEquals(List.of("via logger"), IssueTracker.getErrors());
        assertTrue(IssueTracker.hasErrors());
    }

    @Test
    void loggerWarnDoesNotCollectWhenDisabled() {
        Logger.setCollectWarningsToIssueTracker(false);
        sandbox.execute("logger.warn('not collected')");
        assertFalse(IssueTracker.hasWarnings());
    }

    @Test
    void issuesGlobalIsNotAvailable() {
        assertTrue(sandbox.execute("return issues == nil").toboolean());
    }
}
