package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redactedrice.randomizer.utils.LogLevel;

class ScriptTestCaseTest {

    @Test
    void loadReadsStandardFields(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("sample.lua");
        Files.writeString(caseFile, """
                return {
                    module = "demo_module",
                    seed = 7,
                    args = { count = 2 },
                    extra = "host data",
                }
                """);

        ScriptTestCase testCase = ScriptTestCase.load(caseFile);

        assertEquals("demo_module", testCase.moduleId());
        assertEquals(7, testCase.seed());
        assertEquals(1, testCase.index());
        assertEquals("sample.lua / 1", testCase.displayName());
        assertEquals(2, ScriptTestValues.toInt(testCase.args().get("count"), 0));
        assertEquals("host data", testCase.data().get("extra"));
    }

    @Test
    void loadAllReadsAnArrayOfCases(@TempDir Path tempDir) throws Exception {
        Path caseFile = tempDir.resolve("many.lua");
        Files.writeString(caseFile, """
                return {
                    { name = "first", module = "demo_module", seed = 1 },
                    { name = "second", module = "demo_module", seed = 2 },
                }
                """);

        List<ScriptTestCase> cases = ScriptTestCase.loadAll(caseFile);

        assertEquals(2, cases.size());
        assertEquals("first", ScriptTestValues.optionalString(cases.get(0).data(), "name"));
        assertEquals("many.lua / first", cases.get(0).displayName());
        assertEquals(1, cases.get(0).seed());
        assertEquals("many.lua / second", cases.get(1).displayName());
        assertEquals(2, cases.get(1).seed());
        assertThrows(IllegalArgumentException.class, () -> ScriptTestCase.load(caseFile));
    }

    @Test
    void selectCasesRequiresAFileNameNotAPath(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("test_one.lua"), "return { module = \"x\" }");
        Files.writeString(tempDir.resolve("helper.lua"), "return {}");

        assertEquals(List.of(tempDir.resolve("test_one.lua")),
                ScriptTestCli.selectCases(tempDir, "test_one"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestCli.selectCases(tempDir, "../test_one.lua"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestCli.selectCases(tempDir, "helper"));
        assertEquals(List.of(tempDir.resolve("test_one.lua")),
                ScriptTestCli.selectCases(tempDir, null));
    }

    @Test
    void isLuaCaseRequiresTestPrefix(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("test_case.lua"), "return { module = \"x\" }");
        Files.writeString(tempDir.resolve("helper.lua"), "return {}");
        Files.writeString(tempDir.resolve("01_case.lua"), "return { module = \"x\" }");

        assertTrue(ScriptTestCli.isLuaCase(tempDir.resolve("test_case.lua")));
        assertFalse(ScriptTestCli.isLuaCase(tempDir.resolve("helper.lua")));
        assertFalse(ScriptTestCli.isLuaCase(tempDir.resolve("01_case.lua")));
    }

    @Test
    void selectCasesFindsTestsInSubfolders(@TempDir Path tempDir) throws Exception {
        Path nested = tempDir.resolve("cards");
        Files.createDirectories(nested);
        Files.writeString(tempDir.resolve("test_root.lua"), "return { module = \"x\" }");
        Files.writeString(nested.resolve("test_nested.lua"), "return { module = \"x\" }");
        Files.writeString(nested.resolve("helper.lua"), "return {}");

        List<Path> cases = ScriptTestCli.selectCases(tempDir, null);
        assertEquals(2, cases.size());
        assertTrue(cases.contains(tempDir.resolve("test_root.lua")));
        assertTrue(cases.contains(nested.resolve("test_nested.lua")));
        assertEquals(List.of(nested.resolve("test_nested.lua")),
                ScriptTestCli.selectCases(tempDir, "test_nested"));
    }

    @Test
    void parseRunOptionsDefaultsLogLevelToWarn() {
        ScriptTestCli.RunOptions options =
                ScriptTestCli.parseRunOptions(new String[] { ScriptTestCli.FLAG });
        assertEquals(ScriptTestCli.DEFAULT_LOG_LEVEL, options.logLevel());
        assertEquals(null, options.testFile());
    }

    @Test
    void parseRunOptionsReadsLogLevelAndTestFile() {
        ScriptTestCli.RunOptions options = ScriptTestCli.parseRunOptions(new String[] {
                ScriptTestCli.FLAG, ScriptTestCli.LOG_LEVEL_FLAG, "info", "test_one" });
        assertEquals(LogLevel.INFO, options.logLevel());
        assertEquals("test_one", options.testFile());
    }

    @Test
    void parseRunOptionsRejectsUnknownFlagsAndExtraFiles() {
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestCli.parseRunOptions(new String[] { ScriptTestCli.FLAG, "--nope" }));
        assertThrows(IllegalArgumentException.class, () -> ScriptTestCli.parseRunOptions(
                new String[] { ScriptTestCli.FLAG, "test_one", "test_two" }));
        assertThrows(IllegalArgumentException.class, () -> ScriptTestCli.parseRunOptions(
                new String[] { ScriptTestCli.FLAG, ScriptTestCli.LOG_LEVEL_FLAG }));
    }
}
