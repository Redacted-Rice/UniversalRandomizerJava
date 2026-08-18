package redactedrice.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redactedrice.randomizer.scripttests.ScriptTestCli;

class ExampleScriptTests {
    @Test
    void luaScriptTestsPass() {
        assertEquals(0, ExampleScriptTestRunner.run(new String[] { ScriptTestCli.FLAG }),
                "Lua script tests failed");
    }
}
