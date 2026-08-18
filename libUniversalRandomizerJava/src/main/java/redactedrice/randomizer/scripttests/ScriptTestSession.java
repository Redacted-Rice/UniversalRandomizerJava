package redactedrice.randomizer.scripttests;

import java.util.Map;

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
        JavaContext context = new JavaContext();
        context.mergeEnumRegistry(wrapper.getSharedContext().getEnumRegistry());
        fixtures.populateContext(context, testCase);

        wrapper.executePreRandomizeScripts(context);
        if (IssueTracker.hasErrors()) {
            throw new IllegalStateException("Pre randomize scripts failed in "
                    + testCase.displayName() + ": " + IssueTracker.getErrors());
        }

        runModule(testCase, context, testCase.moduleId(), testCase.args());

        fixtures.assertExpect(testCase, context);
    }

    private void runModule(ScriptTestCase testCase, JavaContext context, String moduleId,
            Map<String, Object> args) {
        Module module = wrapper.getModule(moduleId);
        if (module == null) {
            throw new IllegalStateException(
                    "Unknown module '" + moduleId + "' in " + testCase.displayName());
        }

        ExecutionRequest request = ExecutionRequest.forModule(module, args);
        ExecutionResult result = wrapper.executeModule(request, context, testCase.seed());
        if (!result.isSuccess()) {
            throw new IllegalStateException("Module '" + moduleId + "' failed in "
                    + testCase.displayName() + ": " + result.getErrorMessage());
        }
        if (IssueTracker.hasErrors()) {
            throw new IllegalStateException("Module '" + moduleId + "' reported errors in "
                    + testCase.displayName() + ": " + IssueTracker.getErrors());
        }
    }
}
