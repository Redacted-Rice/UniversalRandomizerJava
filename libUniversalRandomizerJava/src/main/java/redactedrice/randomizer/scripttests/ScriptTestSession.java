package redactedrice.randomizer.scripttests;

import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.utils.IssueTracker;

// Runs one case. The CLI loads every case in a file then calls this per case.
public final class ScriptTestSession {
    private final LuaRandomizerWrapper wrapper;
    private final ScriptTestFixtures fixtures;

    public ScriptTestSession(LuaRandomizerWrapper wrapper, ScriptTestFixtures fixtures) {
        if (wrapper == null) {
            throw new IllegalArgumentException("Wrapper cannot be null");
        }
        if (fixtures == null) {
            throw new IllegalArgumentException("Fixtures cannot be null");
        }
        this.wrapper = wrapper;
        this.fixtures = fixtures;
    }

    public void run(ScriptTestCase testCase) {
        Module module = wrapper.getModule(testCase.moduleId());
        if (module == null) {
            throw new IllegalStateException(
                    "Unknown module '" + testCase.moduleId() + "' in " + testCase.displayName());
        }

        JavaContext context = new JavaContext();
        context.mergeEnumRegistry(wrapper.getSharedContext().getEnumRegistry());
        fixtures.populateContext(context, testCase);

        IssueTracker.clear();
        ExecutionRequest request = ExecutionRequest.forModule(module, testCase.args());
        ExecutionResult result = wrapper.executeModule(request, context, testCase.seed());
        if (!result.isSuccess()) {
            throw new IllegalStateException("Module '" + testCase.moduleId() + "' failed in "
                    + testCase.displayName() + ": " + result.getErrorMessage());
        }
        if (IssueTracker.hasErrors()) {
            throw new IllegalStateException("Module '" + testCase.moduleId()
                    + "' reported errors in " + testCase.displayName() + ": "
                    + IssueTracker.getErrors());
        }

        fixtures.assertExpect(testCase, context);
    }
}
