package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        ScriptTestSession session = new ScriptTestSession(wrapper, noopFixtures());
        ScriptTestRunResult result =
                ScriptTestBatchRunner.runCaseFiles(List.of(caseFile), session);

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

        ScriptTestSession session = new ScriptTestSession(wrapper, fixtures);
        ScriptTestRunResult result =
                ScriptTestBatchRunner.runCaseFiles(List.of(caseFile), session);

        assertEquals(1, result.passed());
        assertEquals(1, result.failed());
        assertFalse(result.isSuccess());
        assertEquals(1, result.exitCode());
        assertEquals("test_many.lua / fails", result.failures().get(0).displayName());
        assertTrue(result.failures().get(0).message().contains("boom"));
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
