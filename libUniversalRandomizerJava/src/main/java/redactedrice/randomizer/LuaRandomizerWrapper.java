package redactedrice.randomizer;

import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.EnumRegistry;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.Logger;
import redactedrice.randomizer.utils.LogLevel;
import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.randomizer.lua.requirements.CoreRequirements;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.lua.ExecutionPlan;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.Issue;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleExecutor;
import redactedrice.randomizer.lua.ModuleRegistry;
import redactedrice.randomizer.lua.dynamicVar.DynamicVarRegistry;

import java.io.OutputStream;
import java.util.*;

// main api for loading and running lua randomizer modules
public class LuaRandomizerWrapper {
    List<String> searchPaths;
    LuaSandbox sandbox;
    ModuleRegistry moduleRegistry;
    ModuleExecutor moduleExecutor;
    JavaContext sharedEnumContext; // shared context for enum registration during onLoad

    public LuaRandomizerWrapper(List<String> allowedDirectories, List<String> searchPaths,
            Set<String> definedGroups, Set<String> definedModifies) {
        this(allowedDirectories, searchPaths, definedGroups, definedModifies, null);
    }

    public LuaRandomizerWrapper(List<String> allowedDirectories, List<String> searchPaths,
            Set<String> definedGroups, Set<String> definedModifies,
            CoreRequirements requirementContext) {
        if (allowedDirectories == null || allowedDirectories.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed directory must be provided");
        }

        this.searchPaths = new ArrayList<>(searchPaths != null ? searchPaths : new ArrayList<>());
        this.sandbox = new LuaSandbox(allowedDirectories);
        this.moduleRegistry =
                new ModuleRegistry(sandbox, definedGroups, definedModifies, requirementContext);
        this.moduleExecutor = new ModuleExecutor(sandbox);
        this.sharedEnumContext = new JavaContext(); // Shared enum context
    }

    public LuaRandomizerWrapper(List<String> allowedDirectories, List<String> searchPaths) {
        this(allowedDirectories, searchPaths, null, null);
    }

    public void addSearchPath(String path) {
        // only add if its a valid path and not already in the list
        if (path != null && !path.trim().isEmpty() && !searchPaths.contains(path)) {
            searchPaths.add(path);
        }
    }

    public void removeSearchPath(String path) {
        searchPaths.remove(path);
    }

    public List<String> getSearchPaths() {
        // return a copy so external modifications dont affect internal state
        return new ArrayList<>(searchPaths);
    }

    public int loadModules() {
        moduleRegistry.clear();
        IssueTracker.snapshot();
        int totalLoaded = 0;

        // Register every module and script before validating requires so filesystem load order
        // does not affect dependency resolution.
        for (String path : searchPaths) {
            totalLoaded += moduleRegistry.loadModulesFromDirectory(path);
        }

        // Validate requirements for loaded modules
        moduleRegistry.validateAllRequirements();
        callModuleOnLoadFunctions();
        IssueTracker.logDeltaSummary("Module load");
        IssueTracker.clearSnapshot();
        return totalLoaded;
    }

    private void callModuleOnLoadFunctions() {
        // call each modules onLoad function if it has one
        for (Module module : getAvailableModules()) {
            if (module.hasOnLoad()) {
                try {
                    // create a context with registered enum available
                    org.luaj.vm2.LuaTable contextTable = sharedEnumContext.toLuaTable();

                    // call onload with context
                    module.getOnLoadFunction().call(contextTable);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("[LuaRandomizerWrapper] Error calling onLoad for module '"
                            + module.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    public List<Module> getAvailableModules() {
        return moduleRegistry.getAllModules();
    }

    public Set<String> getModuleIds() {
        return moduleRegistry.getModuleIds();
    }

    public Module getModule(String moduleId) {
        return moduleRegistry.getModule(moduleId);
    }

    public Module getScript(String moduleId) {
        return moduleRegistry.getScript(moduleId);
    }

    // TODO: Keep these exposed and remove delegating fns or remove these and add more
    // delegating fns?
    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    public JavaContext getSharedContext() {
        return sharedEnumContext;
    }

    // Start of a randomize batch manually executed piece by piece. This clears prior execution
    // results and issues so the host can accumulate across executeModule calls, then read
    // IssueTracker later if desired.
    public void executePreRandomizeScripts(JavaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        // Ensure enums are up to date
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());

        // Clear previous results
        moduleExecutor.clearResults();
        IssueTracker.clear();
        // get the pre randomize scripts and run them
        List<Module> preRandomizeScripts = moduleRegistry
                .getScripts(ModuleRegistry.SCRIPT_TIMING_PRE, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);
        moduleExecutor.executeScripts(preRandomizeScripts, context,
                ModuleRegistry.SCRIPT_TIMING_PRE, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);
    }

    public void executePostRandomizeScripts(JavaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        // Ensure enums are up to date
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());

        // get the post randomize scripts and run them
        List<Module> postRandomizeScripts = moduleRegistry.getScripts(
                ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);
        moduleExecutor.executeScripts(postRandomizeScripts, context,
                ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);
    }

    // Full randomize batch. This clears prior results/issues once, runs all modules/scripts, then
    // leaves issues in IssueTracker so the host can read them later if desired.
    public List<ExecutionResult> executeModules(List<ExecutionRequest> requests,
            JavaContext context, int baseSeed) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Requests list cannot be null or empty");
        }
        // add the shared enum registry from onLoad to the execution context
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());
        // Clear any old wrappers
        context.clearWrapperCache();

        // Clear any previous results or errors and create/validate the execution plan
        moduleExecutor.clearResults();
        IssueTracker.clear();

        ExecutionPlan plan = ExecutionPlan.forRandomizeBatch(moduleRegistry, requests);
        if (!plan.validate()) {
            return List.of();
        }

        // Execute the pre randomize scripts
        moduleExecutor.executeScripts(plan.getPreRandomizeScripts(), context,
                ModuleRegistry.SCRIPT_TIMING_PRE, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);

        // Execute the modules running the pre/post scripts for each one
        List<ExecutionResult> results =
                moduleExecutor.executeModules(plan.getModuleRequests(), moduleRegistry, context,
                        plan.getPreModuleScripts(), plan.getPostModuleScripts(), baseSeed);

        // Execute post randomize scripts
        moduleExecutor.executeScripts(plan.getPostRandomizeScripts(), context,
                ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);

        return results;
    }

    // Single module (plus its pre/post module scripts). Does not clear issues - call
    // IssueTracker.clear or executePreRandomizeScripts once before a multi module host loop.
    // Dynamic var order is not validated here. For one by one loops the host should build and
    // validate an ExecutionPlan for the full request list up front (or revalidate as actions are
    // added) via createExecutionPlan / validateExecutionPlan.
    public ExecutionResult executeModule(ExecutionRequest request, JavaContext context,
            int baseSeed) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        // add the shared enum registry from onLoad to the execution context
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());

        // Execute the module with only pre/post module scripts
        // Do not validate here but assume outside orchestrator is validating the overall plan
        ExecutionPlan plan = ExecutionPlan.forSingleModule(moduleRegistry, request);
        List<ExecutionResult> results =
                moduleExecutor.executeModules(plan.getModuleRequests(), moduleRegistry, context,
                        plan.getPreModuleScripts(), plan.getPostModuleScripts(), baseSeed);

        return results.get(0);
    }

    // Will return module and scrupt results
    public List<ExecutionResult> getExecutionResults() {
        return moduleExecutor.getResults();
    }

    public void clearExecutionResults() {
        moduleExecutor.clearResults();
    }

    public void printModuleSummary() {
        // print summary of all loaded modules and their metadata
        List<Module> modules = getAvailableModules();
        System.out.println("=== Loaded Modules ===");
        System.out.println("Total: " + modules.size());
        System.out.println();

        // print each module's details
        for (Module module : modules) {
            System.out.println("Module: " + module.getId() + " (" + module.getName() + ")");
            System.out.println("  Description: " + module.getDescription());
            if (!module.getGroups().isEmpty()) {
                System.out.println("  Groups: " + module.getGroups());
            }
            if (!module.getModifies().isEmpty()) {
                System.out.println("  Modifies: " + module.getModifies());
            }
            System.out.println("  Arguments: " + module.getArguments().size());
            module.getArguments().forEach(arg -> {
                String defaultInfo =
                        arg.getDefaultValue() != null ? " (default: " + arg.getDefaultValue() + ")"
                                : "";
                System.out.println("    - " + arg.getName() + " (" + arg.getTypeDefinition() + "): "
                        + arg.getConstraint().getDescription() + defaultInfo);
            });
            System.out.println("  File: " + module.getFilePath());
            System.out.println();
        }
    }

    public EnumDefinition getEnumDefinition(String enumName) {
        if (enumName == null || enumName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        return sharedEnumContext.getEnumRegistry().getEnum(enumName);
    }

    public Set<String> getRegisteredEnumNames() {
        return sharedEnumContext.getEnumRegistry().getEnumNames();
    }

    public LuaSandbox getSandbox() {
        return sandbox;
    }

    public Set<String> getDefinedGroupValues() {
        return moduleRegistry.getDefinedGroupValues();
    }

    public List<Module> getModulesByGroup(String group) {
        return moduleRegistry.getModulesByGroup(group);
    }

    public Set<String> getDefinedModifiesValues() {
        return moduleRegistry.getDefinedModifiesValues();
    }

    public List<Module> getModulesByModifies(String modifies) {
        return moduleRegistry.getModulesByModifies(modifies);
    }

    public DynamicVarRegistry getDynamicVarRegistry() {
        return moduleRegistry.getDynamicVarRegistry();
    }

    /**
     * Builds the run plan for a full randomize batch. Hosts that execute modules one by one should
     * create this from the complete (or growing) request list and call ExecutionPlan.validate
     * before the loop, revalidating whenever the list changes.
     */
    public ExecutionPlan createExecutionPlan(List<ExecutionRequest> requests) {
        return ExecutionPlan.forRandomizeBatch(moduleRegistry, requests);
    }

    /**
     * Returns dynamic var execution order issues without reporting them to IssueTracker. Use
     * createExecutionPlan and ExecutionPlan.validate when the host wants validation errors logged
     * for popups.
     */
    public List<Issue> validateExecutionPlan(List<ExecutionRequest> requests) {
        return ExecutionPlan.collectDynamicVarExecutionIssues(moduleRegistry, requests);
    }
}
