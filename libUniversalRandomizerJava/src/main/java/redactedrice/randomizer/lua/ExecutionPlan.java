package redactedrice.randomizer.lua;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redactedrice.randomizer.lua.dynamicVar.DynamicVarValidator;
import redactedrice.randomizer.utils.IssueTracker;

/**
 * Ordered run plan for a randomize batch or module only call. Built from the registry and request
 * list. Used for dynamic var validation and for driving batch execution.
 *
 * Hosts that add actions incrementally can rebuild with forRandomizeBatch as the list grows and
 * call validate before running.
 */
public final class ExecutionPlan {
    private final List<Module> preRandomizeScripts;
    private final List<ExecutionRequest> moduleRequests;
    private final List<Module> preModuleScripts;
    private final List<Module> postModuleScripts;
    private final List<Module> postRandomizeScripts;
    private final List<Module> steps;

    private ExecutionPlan(ModuleRegistry moduleRegistry, List<Module> preRandomizeScripts,
            List<ExecutionRequest> moduleRequests, List<Module> preModuleScripts,
            List<Module> postModuleScripts, List<Module> postRandomizeScripts) {
        this.preRandomizeScripts = List.copyOf(preRandomizeScripts);
        this.moduleRequests = List.copyOf(moduleRequests);
        this.preModuleScripts = List.copyOf(preModuleScripts);
        this.postModuleScripts = List.copyOf(postModuleScripts);
        this.postRandomizeScripts = List.copyOf(postRandomizeScripts);
        this.steps = buildSteps(moduleRegistry);
    }

    private ExecutionPlan(List<Module> steps) {
        this.preRandomizeScripts = List.of();
        this.moduleRequests = List.of();
        this.preModuleScripts = List.of();
        this.postModuleScripts = List.of();
        this.postRandomizeScripts = List.of();
        this.steps = List.copyOf(steps);
    }

    public static ExecutionPlan forRandomizeBatch(ModuleRegistry moduleRegistry,
            List<ExecutionRequest> requests) {
        return buildRandomizeBatch(moduleRegistry, requests);
    }

    public static ExecutionPlan forSingleModule(ModuleRegistry moduleRegistry,
            ExecutionRequest request) {
        return buildModuleScope(moduleRegistry, request != null ? List.of(request) : List.of());
    }

    public static ExecutionPlan forModuleScope(ModuleRegistry moduleRegistry,
            List<ExecutionRequest> requests) {
        return buildModuleScope(moduleRegistry, requests);
    }

    public static ExecutionPlan fromSteps(List<Module> steps) {
        return new ExecutionPlan(steps);
    }

    public static List<Issue> collectDynamicVarExecutionIssues(ModuleRegistry moduleRegistry,
            List<ExecutionRequest> requests) {
        return DynamicVarValidator
                .validateExecutionPlan(buildRandomizeBatch(moduleRegistry, requests), null);
    }

    static ExecutionPlan buildRandomizeBatch(ModuleRegistry moduleRegistry,
            List<ExecutionRequest> requests) {
        return new ExecutionPlan(moduleRegistry,
                moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_PRE,
                        ModuleRegistry.SCRIPT_WHEN_RANDOMIZE),
                requests != null ? requests : List.of(),
                moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_PRE,
                        ModuleRegistry.SCRIPT_WHEN_MODULE),
                moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_POST,
                        ModuleRegistry.SCRIPT_WHEN_MODULE),
                moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_POST,
                        ModuleRegistry.SCRIPT_WHEN_RANDOMIZE));
    }

    static ExecutionPlan buildModuleScope(ModuleRegistry moduleRegistry,
            List<ExecutionRequest> requests) {
        return new ExecutionPlan(moduleRegistry, List.of(), requests != null ? requests : List.of(),
                moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_PRE,
                        ModuleRegistry.SCRIPT_WHEN_MODULE),
                moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_POST,
                        ModuleRegistry.SCRIPT_WHEN_MODULE),
                List.of());
    }

    public boolean validate() {
        List<Issue> issues = DynamicVarValidator.validateExecutionPlan(this, null);
        boolean valid = true;
        for (Issue issue : issues) {
            if (issue.isError()) {
                IssueTracker.addError(issue.getCategory(), issue.getMessage());
                valid = false;
            } else {
                IssueTracker.addWarning(issue.getCategory(), issue.getMessage());
            }
        }
        return valid;
    }

    private static List<Module> buildSteps(ModuleRegistry moduleRegistry,
            List<Module> preRandomizeScripts, List<ExecutionRequest> moduleRequests,
            List<Module> preModuleScripts, List<Module> postModuleScripts,
            List<Module> postRandomizeScripts) {
        List<Module> orderedSteps = new ArrayList<>();
        orderedSteps.addAll(preRandomizeScripts);
        for (ExecutionRequest request : moduleRequests) {
            orderedSteps.addAll(preModuleScripts);
            Module module = resolveModule(moduleRegistry, request);
            if (module != null) {
                orderedSteps.add(module);
            }
            orderedSteps.addAll(postModuleScripts);
        }
        orderedSteps.addAll(postRandomizeScripts);
        return List.copyOf(orderedSteps);
    }

    private List<Module> buildSteps(ModuleRegistry moduleRegistry) {
        return buildSteps(moduleRegistry, preRandomizeScripts, moduleRequests, preModuleScripts,
                postModuleScripts, postRandomizeScripts);
    }

    private static Module resolveModule(ModuleRegistry moduleRegistry, ExecutionRequest request) {
        if (request == null || moduleRegistry == null) {
            return null;
        }
        Module module = moduleRegistry.getModule(request.getModuleId());
        if (module != null) {
            return module;
        }
        return moduleRegistry.getScript(request.getModuleId());
    }

    public List<Module> getPreRandomizeScripts() {
        return preRandomizeScripts;
    }

    public List<ExecutionRequest> getModuleRequests() {
        return moduleRequests;
    }

    public List<Module> getPreModuleScripts() {
        return preModuleScripts;
    }

    public List<Module> getPostModuleScripts() {
        return postModuleScripts;
    }

    public List<Module> getPostRandomizeScripts() {
        return postRandomizeScripts;
    }

    public List<Module> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
