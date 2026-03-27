package redactedrice.randomizer;

import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.EnumRegistry;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.Logger;
import redactedrice.randomizer.utils.LogLevel;
import redactedrice.randomizer.utils.ErrorTracker;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRegistry;
import redactedrice.randomizer.lua.ModuleExecutor;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.ExecutionResult;

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
        if (allowedDirectories == null || allowedDirectories.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed directory must be provided");
        }

        this.searchPaths = new ArrayList<>(searchPaths != null ? searchPaths : new ArrayList<>());

        this.sandbox = new LuaSandbox(allowedDirectories);
        this.moduleRegistry = new ModuleRegistry(sandbox, definedGroups, definedModifies);
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
        int totalLoaded = 0;

        // load modules from all the search paths
        for (String path : searchPaths) {
            int loaded = moduleRegistry.loadModulesFromDirectory(path);
            totalLoaded += loaded;
        }

        // call onLoad functions if modules have them
        callModuleOnLoadFunctions();

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

    public Set<String> getModuleNames() {
        return moduleRegistry.getModuleNames();
    }

    public Module getModule(String name) {
        return moduleRegistry.getModule(name);
    }

    // TODO: Keep these exposed and remove delegating fns or remove these and add more
    // delegating fns?
    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    public JavaContext getSharedContext() {
        return sharedEnumContext;
    }

    public void executePreRandomizeScripts(JavaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        // Ensure enums are up to date
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());

        // Clear previous results
        moduleExecutor.clearResults();
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

    // Will return only the module results, not the script results
    public List<ExecutionResult> executeModules(List<ExecutionRequest> requests,
            JavaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Requests list cannot be null or empty");
        }

        // add the shared enum registry from onLoad to the execution context
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());

        // get scripts by timing and when
        List<Module> preRandomizeScripts = moduleRegistry
                .getScripts(ModuleRegistry.SCRIPT_TIMING_PRE, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);
        List<Module> preModuleScripts = moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_PRE,
                ModuleRegistry.SCRIPT_WHEN_MODULE);
        List<Module> postModuleScripts = moduleRegistry
                .getScripts(ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_MODULE);
        List<Module> postRandomizeScripts = moduleRegistry.getScripts(
                ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);

        // Clear results and execute pre randomize scripts
        moduleExecutor.clearResults();
        moduleExecutor.executeScripts(preRandomizeScripts, context,
                ModuleRegistry.SCRIPT_TIMING_PRE, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);

        // Execute the modules running the pre/post scripts for each one
        List<ExecutionResult> results = moduleExecutor.executeModules(requests, moduleRegistry,
                context, preModuleScripts, postModuleScripts);

        // Execute post randomize scripts
        moduleExecutor.executeScripts(postRandomizeScripts, context,
                ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_RANDOMIZE);

        return results;
    }

    // Will return only the module result, not the script results
    public ExecutionResult executeModule(ExecutionRequest request, JavaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        // add the shared enum registry from onLoad to the execution context
        context.mergeEnumRegistry(sharedEnumContext.getEnumRegistry());

        // get only module level scripts. Randomize level must be called by the caller
        List<Module> preModuleScripts = moduleRegistry.getScripts(ModuleRegistry.SCRIPT_TIMING_PRE,
                ModuleRegistry.SCRIPT_WHEN_MODULE);
        List<Module> postModuleScripts = moduleRegistry
                .getScripts(ModuleRegistry.SCRIPT_TIMING_POST, ModuleRegistry.SCRIPT_WHEN_MODULE);

        // Execute the module with only pre/post module scripts
        List<ExecutionResult> results =
                moduleExecutor.executeModules(Collections.singletonList(request), moduleRegistry,
                        context, preModuleScripts, postModuleScripts);

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
            System.out.println("Module: " + module.getName());
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

        // print any errors encountered during loading
        if (ErrorTracker.hasErrors()) {
            System.out.println("=== Load Errors ===");
            ErrorTracker.getErrors().forEach(System.out::println);
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
}

