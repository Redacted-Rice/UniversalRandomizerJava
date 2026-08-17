package redactedrice.randomizer.scripttests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        assertEquals(2, ScriptTestValues.toInt(testCase.args().get("count"), 0));
        assertEquals("host data", testCase.data().get("extra"));
    }

    @Test
    void selectCasesRequiresAFileNameNotAPath(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("one.lua"), "return { module = \"x\" }");

        assertEquals(List.of(tempDir.resolve("one.lua")),
                ScriptTestCli.selectCases(tempDir, "one"));
        assertThrows(IllegalArgumentException.class,
                () -> ScriptTestCli.selectCases(tempDir, "../one.lua"));
        assertTrue(ScriptTestCli.selectCases(tempDir, null).contains(tempDir.resolve("one.lua")));
    }
}
