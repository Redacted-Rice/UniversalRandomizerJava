package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.support.test.TestEntity;

class ScriptTestBatchRunnerTest {
    private LuaRandomizerWrapper wrapper;

    @BeforeEach
    void setUp() {
        String randomizerPath = new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        String modulesPath = new File("src/test/java/redactedrice/support/lua_modules")
                .getAbsolutePath();
        wrapper = new LuaRandomizerWrapper(
                List.of(randomizerPath, modulesPath),
                List.of(modulesPath));
        IssueTracker.clear();
        wrapper.loadModules();
    }

    @Test
    void runCaseFilesReportsLoadFailure(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("test_bad.lua");
        Files.writeString(caseFile, "return { not valid");

        ScriptTestRunResult result = ScriptTestBatchRunner.runCaseFiles(List.of(caseFile),
                () -> wrapper, noopFixtures());

        assertEquals(0, result.passed());
        assertEquals(1, result.failed());
        assertFalse(result.isSuccess());
        assertEquals("test_bad.lua", result.failures().get(0).displayName());
    }

    @Test
    void runCaseFilesCollectsFailuresWithDisplayNames(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("test_many.lua");
        Files.writeString(caseFile, """
                return {
                    { name = "passes", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                    { name = "fails", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 999 } },
                }
                """);

        ScriptTestFixtures fixtures = new ScriptTestFixtures() {
            @Override
            public void populateContext(JavaContext context, ScriptTestCase testCase) {
                context.register("entity", new TestEntity("Hero", 100, 10.0, true));
            }

            @Override
            public void assertExpect(ScriptTestCase testCase, JavaContext context) {
                TestEntity entity = (TestEntity) context.get("entity");
                var expect = ScriptTestValues.optionalMap(testCase.data().get("expect"));
                if (expect != null && Integer.valueOf(999).equals(expect.get("health"))) {
                    throw new IllegalStateException("boom");
                }
            }
        };

        ScriptTestRunResult result = ScriptTestBatchRunner.runCaseFiles(List.of(caseFile),
                () -> wrapper, fixtures);

        assertEquals(1, result.passed());
        assertEquals(1, result.failed());
        assertFalse(result.isSuccess());
        assertEquals(1, result.exitCode());
        assertEquals("test_many.lua / fails", result.failures().get(0).displayName());
        assertTrue(result.failures().get(0).message().contains("boom"));
    }

    @Test
    void runCaseFilesInvokesFactoryOncePerCase(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("test_two.lua");
        Files.writeString(caseFile, """
                return {
                    { name = "one", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                    { name = "two", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                }
                """);

        AtomicInteger createCount = new AtomicInteger();
        ScriptTestRunResult result = ScriptTestBatchRunner.runCaseFiles(List.of(caseFile),
                () -> {
                    createCount.incrementAndGet();
                    return wrapper;
                }, entityFixtures());

        assertEquals(2, result.passed());
        assertEquals(0, result.failed());
        assertEquals(2, createCount.get());
    }

    @Test
    void runCaseFilesClearsIssueTrackerBetweenCasesWithSharedWrapper(@TempDir Path tempDir)
            throws Exception {
        Path caseFile = tempDir.resolve("test_pair.lua");
        Files.writeString(caseFile, """
                return {
                    { name = "first", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                    { name = "second", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                }
                """);

        IssueTracker.addError("stale from before the batch");
        ScriptTestRunResult result = ScriptTestBatchRunner.runCaseFiles(List.of(caseFile),
                () -> wrapper, entityFixtures());

        assertEquals(2, result.passed());
        assertEquals(0, result.failed());
        assertTrue(result.isSuccess());
    }

    @Test
    void runCaseFilesCountsWrapperFactoryFailureAsCaseFailure(@TempDir Path tempDir)
            throws Exception {
        Path caseFile = tempDir.resolve("test_factory.lua");
        Files.writeString(caseFile, """
                return {
                    { name = "ok", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                    { name = "broken factory", module = "simple_entity_randomizer", seed = 0,
                      args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                      expect = { health = 50, damage = 20.0 } },
                }
                """);

        AtomicInteger createCount = new AtomicInteger();
        ScriptTestRunResult result = ScriptTestBatchRunner.runCaseFiles(List.of(caseFile),
                () -> {
                    if (createCount.incrementAndGet() == 2) {
                        throw new IllegalStateException("wrapper setup failed");
                    }
                    return wrapper;
                }, entityFixtures());

        assertEquals(1, result.passed());
        assertEquals(1, result.failed());
        assertEquals("test_factory.lua / broken factory", result.failures().get(0).displayName());
        assertTrue(result.failures().get(0).message().contains("wrapper setup failed"));
    }

    private static ScriptTestFixtures entityFixtures() {
        return new ScriptTestFixtures() {
            @Override
            public void populateContext(JavaContext context, ScriptTestCase testCase) {
                context.register("entity", new TestEntity("Hero", 100, 10.0, true));
            }

            @Override
            public void assertExpect(ScriptTestCase testCase, JavaContext context) {
                TestEntity entity = (TestEntity) context.get("entity");
                var expect = ScriptTestValues.optionalMap(testCase.data().get("expect"));
                if (expect != null && expect.get("health") instanceof Number health) {
                    assertEquals(health.intValue(), entity.getHealth());
                }
                if (expect != null && expect.get("damage") instanceof Number damage) {
                    assertEquals(damage.doubleValue(), entity.getDamage());
                }
            }
        };
    }

    private static ScriptTestFixtures noopFixtures() {
        return new ScriptTestFixtures() {
            @Override
            public void populateContext(JavaContext context, ScriptTestCase testCase) {
            }

            @Override
            public void assertExpect(ScriptTestCase testCase, JavaContext context) {
            }
        };
    }
}
