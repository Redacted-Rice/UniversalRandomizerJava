package redactedrice.randomizer.scripttests;

import java.util.Map;

import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.utils.IssueTracker;

// Runs one case. Same pre/module/post randomize scripts as a live batch, for one module.
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
        IssueTracker.clear();
        JavaContext executionContext = new JavaContext();
        wrapper.prepareContext(executionContext);
        executionContext.setConfig(JavaContext.CHANGE_DETECTION_ACTIVE, true);
        fixtures.populateContext(executionContext, testCase);

        wrapper.executePreRandomizeScripts(executionContext);
        failIfIssues("Pre randomize scripts failed", testCase);

        runModule(testCase, executionContext, testCase.moduleId(), testCase.args());

        wrapper.executePostRandomizeScripts(executionContext);
        failIfIssues("Post randomize scripts failed", testCase);

        fixtures.assertExpect(testCase, executionContext);
    }

    private void failIfIssues(String what, ScriptTestCase testCase) {
        if (IssueTracker.hasErrors()) {
            throw new IllegalStateException(
                    what + " in " + testCase.displayName() + ": " + IssueTracker.getErrors());
        }
    }

    private void runModule(ScriptTestCase testCase, JavaContext executionContext, String moduleId,
            Map<String, Object> args) {
        Module module = wrapper.getModule(moduleId);
        if (module == null) {
            throw new IllegalStateException(
                    "Unknown module '" + moduleId + "' in " + testCase.displayName());
        }

        ExecutionRequest request = ExecutionRequest.forModule(module, args);
        ExecutionResult result =
                wrapper.executeModule(request, executionContext, testCase.seed());
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
