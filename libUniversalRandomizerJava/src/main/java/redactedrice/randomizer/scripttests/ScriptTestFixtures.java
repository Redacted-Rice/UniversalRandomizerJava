package redactedrice.randomizer.scripttests;

import redactedrice.randomizer.context.JavaContext;

// Host fills the Lua context and checks results. Keep game objects out of URJ.
public interface ScriptTestFixtures {
    void populateContext(JavaContext context, ScriptTestCase testCase);

    void assertExpect(ScriptTestCase testCase, JavaContext context);
}
