package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.support.test.TestEntity;

class ScriptTestSessionTest {

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
    void runExecutesModuleAndFixtures(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("01_entity.lua");
        Files.writeString(caseFile, """
                return {
                    module = "simple_entity_randomizer",
                    seed = 0,
                    args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                    expect = { health = 50, damage = 20.0 },
                }
                """);

        AtomicBoolean populated = new AtomicBoolean();
        AtomicBoolean asserted = new AtomicBoolean();
        ScriptTestFixtures fixtures = new ScriptTestFixtures() {
            @Override
            public void populateContext(JavaContext context, ScriptTestCase testCase) {
                populated.set(true);
                context.register("entity", new TestEntity("Hero", 100, 10.0, true));
            }

            @Override
            public void assertExpect(ScriptTestCase testCase, JavaContext context) {
                asserted.set(true);
                TestEntity entity = (TestEntity) context.get("entity");
                Map<String, Object> expect =
                        ScriptTestValues.optionalMap(testCase.data().get("expect"));
                assertEquals(expect.get("health"), entity.getHealth());
                assertEquals(((Number) expect.get("damage")).doubleValue(), entity.getDamage(),
                        0.001);
            }
        };

        ScriptTestSession session = new ScriptTestSession(wrapper, fixtures);
        session.run(ScriptTestCase.load(caseFile));

        assertTrue(populated.get());
        assertTrue(asserted.get());
    }

    @Test
    void runFailsForUnknownModule(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("01_missing.lua");
        Files.writeString(caseFile, """
                return {
                    module = "not_a_real_module",
                    seed = 0,
                }
                """);

        ScriptTestFixtures fixtures = new ScriptTestFixtures() {
            @Override
            public void populateContext(JavaContext context, ScriptTestCase testCase) {
                context.register("entity", new TestEntity());
            }

            @Override
            public void assertExpect(ScriptTestCase testCase, JavaContext context) {
            }
        };

        ScriptTestSession session = new ScriptTestSession(wrapper, fixtures);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> session.run(ScriptTestCase.load(caseFile)));
        assertTrue(error.getMessage().contains("Unknown module"));
    }

    @Test
    void runFailsWhenAssertExpectFindsMismatches(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("01_bad_expect.lua");
        Files.writeString(caseFile, """
                return {
                    module = "simple_entity_randomizer",
                    seed = 0,
                    args = { healthMin = 50, healthMax = 50, damageMultiplier = 2.0 },
                    expect = { health = 999 },
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
                List<String> mismatches = new ArrayList<>();
                ScriptTestFields.collectMismatches(context, entity,
                        ScriptTestValues.optionalMap(testCase.data().get("expect")),
                        mismatches, "entity");
                ScriptTestFields.failIfMismatches(testCase.displayName(), mismatches);
            }
        };

        ScriptTestSession session = new ScriptTestSession(wrapper, fixtures);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> session.run(ScriptTestCase.load(caseFile)));
        assertTrue(error.getMessage().contains("health expected 999"));
    }
}
