package redactedrice.randomizer.wrapper;

import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.context.PseudoEnumRegistry;
import redactedrice.randomizer.logger.Logger;
import redactedrice.randomizer.metadata.LuaModuleMetadata;

import java.io.OutputStream;
import java.util.*;

// main api for loading and running lua randomizer modules
public class LuaRandomizerWrapper {
    String randomizerPath;
    List<String> searchPaths;
    LuaSandbox sandbox;
    LuaModuleLoader moduleLoader;
    ModuleExecutor moduleExecutor;
    ChangeDetector changeDetector;
    PseudoEnumRegistry pseudoEnumRegistry;
    JavaContext sharedEnumContext; // shared context for enum registration during onLoad
    List<Object> monitoredObjects;

    public LuaRandomizerWrapper(List<String> searchPaths, PseudoEnumRegistry pseudoEnumRegistry) {
        this.randomizerPath = RandomizerResourceExtractor.getPath();
        this.searchPaths = new ArrayList<>(searchPaths != null ? searchPaths : new ArrayList<>());
        this.sandbox = new LuaSandbox(randomizerPath);
        this.moduleLoader = new LuaModuleLoader(sandbox);
        this.moduleExecutor = new ModuleExecutor(sandbox);
        this.changeDetector = new ChangeDetector();
        this.pseudoEnumRegistry =
                pseudoEnumRegistry != null ? pseudoEnumRegistry : new PseudoEnumRegistry();
        this.sharedEnumContext = new JavaContext(); // Shared enum context
        this.monitoredObjects = new ArrayList<>();
    }

    public LuaRandomizerWrapper(List<String> searchPaths) {
        this(searchPaths, null);
    }

    public LuaRandomizerWrapper(String searchPath) {
        this(searchPath != null ? Collections.singletonList(searchPath) : null);
    }

    public LuaRandomizerWrapper() {
        // Cast to use the right other constructor
        this((List<String>) null);
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
        moduleLoader.clear();
        int totalLoaded = 0;

        // load modules from all the search paths
        for (String path : searchPaths) {
            int loaded = moduleLoader.loadModulesFromDirectory(path);
            totalLoaded += loaded;
        }

        // register all the group and modifies enum values
        registerModulePseudoEnums();

        // call onLoad functions if modules have them
        callModuleOnLoadFunctions();

        return totalLoaded;
    }

    private void callModuleOnLoadFunctions() {
        // call each modules onLoad function if it has one
        for (LuaModuleMetadata module : getAvailableModules()) {
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

    public void registerModulePseudoEnums() {
        // go through all modules and register their group and modifies values
        for (LuaModuleMetadata module : getAvailableModules()) {
            // register module group
            if (module.getGroup() != null && !module.getGroup().isEmpty()) {
                pseudoEnumRegistry.extendEnum("ModuleGroup", module.getGroup());
            }

            // register module modifies
            for (String modifyValue : module.getModifies()) {
                if (modifyValue != null && !modifyValue.isEmpty()) {
                    pseudoEnumRegistry.extendEnum("ModuleModifies", modifyValue);
                }
            }
        }
    }

    public List<LuaModuleMetadata> getAvailableModules() {
        return moduleLoader.getAllModules();
    }

    public Set<String> getModuleNames() {
        return moduleLoader.getModuleNames();
    }

    public LuaModuleMetadata getModule(String name) {
        return moduleLoader.getModule(name);
    }

    public void setMonitoredObjects(List<Object> objects) {
        this.monitoredObjects = objects != null ? new ArrayList<>(objects) : new ArrayList<>();
    }

    public void setMonitoredObjects(Object... objects) {
        this.monitoredObjects = objects != null ? Arrays.asList(objects) : new ArrayList<>();
    }

    public void setChangeDetectionEnabled(boolean enabled) {
        changeDetector.setEnabled(enabled);
    }

    public boolean isChangeDetectionEnabled() {
        return changeDetector.isEnabled();
    }

    public List<ExecutionResult> executeModules(List<String> moduleNames, JavaContext context,
            Map<String, Map<String, Object>> argumentsPerModule,
            Map<String, Integer> seedsPerModule) {
        if (moduleNames == null || moduleNames.isEmpty()) {
            throw new IllegalArgumentException("Module names list cannot be null or empty");
        }

        // use empty context if none given
        if (context == null) {
            context = new JavaContext();
        }

        // add the shared enum context from onLoad to the execution context
        context.mergeEnumContext(sharedEnumContext.getEnumContext());

        // use empty args map if none given
        if (argumentsPerModule == null) {
            argumentsPerModule = new HashMap<>();
        }

        // find all the modules by name
        List<LuaModuleMetadata> modulesToExecute = new ArrayList<>();
        for (String name : moduleNames) {
            LuaModuleMetadata module = moduleLoader.getModule(name);
            if (module == null) {
                throw new IllegalArgumentException("Module not found: " + name);
            }
            modulesToExecute.add(module);
        }

        // run the modules
        moduleExecutor.clear();
        return moduleExecutor.executeModules(modulesToExecute, context, argumentsPerModule,
                seedsPerModule, changeDetector, monitoredObjects);
    }

    public List<ExecutionResult> executeModules(List<String> moduleNames, JavaContext context,
            Map<String, Map<String, Object>> argumentsPerModule) {
        return executeModules(moduleNames, context, argumentsPerModule, null);
    }

    public ExecutionResult executeModule(String moduleName, JavaContext context,
            Map<String, Object> arguments, Integer seed) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }

        // find the module by name
        LuaModuleMetadata module = moduleLoader.getModule(moduleName);
        if (module == null) {
            throw new IllegalArgumentException("Module not found: " + moduleName);
        }

        // use empty context if none given
        if (context == null) {
            context = new JavaContext();
        }

        // add the shared enum context from onLoad to the execution context
        context.mergeEnumContext(sharedEnumContext.getEnumContext());

        // use empty args if none given
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        moduleExecutor.clear();
        return moduleExecutor.executeModule(module, context, arguments, seed, changeDetector,
                monitoredObjects);
    }

    public ExecutionResult executeModule(String moduleName, JavaContext context,
            Map<String, Object> arguments) {
        return executeModule(moduleName, context, arguments, null);
    }

    public List<String> getLoadErrors() {
        return moduleLoader.getErrors();
    }

    public List<String> getExecutionErrors() {
        return moduleExecutor.getErrors();
    }

    public List<String> getAllErrors() {
        List<String> allErrors = new ArrayList<>();
        allErrors.addAll(moduleLoader.getErrors());
        allErrors.addAll(moduleExecutor.getErrors());
        return allErrors;
    }

    public boolean hasErrors() {
        return moduleLoader.hasErrors() || moduleExecutor.hasErrors();
    }

    public void printModuleSummary() {
        // print summary of all loaded modules and their metadata
        List<LuaModuleMetadata> modules = getAvailableModules();
        System.out.println("=== Loaded Modules ===");
        System.out.println("Total: " + modules.size());
        System.out.println();

        // print each module's details
        for (LuaModuleMetadata module : modules) {
            System.out.println("Module: " + module.getName());
            System.out.println("  Description: " + module.getDescription());
            System.out.println("  Group: " + module.getGroup());
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
        if (moduleLoader.hasErrors()) {
            System.out.println("=== Load Errors ===");
            moduleLoader.getErrors().forEach(System.out::println);
            System.out.println();
        }
    }

    public PseudoEnumRegistry getPseudoEnumRegistry() {
        return pseudoEnumRegistry;
    }

    public EnumDefinition getEnumDefinition(String enumName) {
        if (enumName == null || enumName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        return sharedEnumContext.getEnumContext().getEnum(enumName);
    }

    public Set<String> getRegisteredEnumNames() {
        return sharedEnumContext.getEnumContext().getEnumNames();
    }

    public LuaSandbox getSandbox() {
        return sandbox;
    }

    // ========== Logger Configuration Methods ==========

    public void setLogEnabled(boolean enabled) {
        Logger.setEnabled(enabled);
    }

    public boolean isLogEnabled() {
        return Logger.isEnabled();
    }

    public void addStreamForLogLevel(Logger.LogLevel level, OutputStream stream) {
        Logger.addStreamForLevel(level, stream);
    }

    public void addStreamForLogLevels(OutputStream stream, Logger.LogLevel... levels) {
        Logger.addStreamForLevels(stream, levels);
    }

    public void addStreamForAllLogLevels(OutputStream stream) {
        Logger.addStreamForAllLevels(stream);
    }

    public void removeAllStreamsForLogLevel(Logger.LogLevel level) {
        Logger.removeAllStreamsForLevel(level);
    }

    public void setShowLogTimestamp(boolean show) {
        Logger.setShowTimestamp(show);
    }

    public void setShowLogModuleName(boolean show) {
        Logger.setShowModuleName(show);
    }

    public void setLogMinLevel(Logger.LogLevel level) {
        Logger.setMinLogLevel(level);
    }

    public Logger.LogLevel getLogMinLevel() {
        return Logger.getMinLogLevel();
    }

    public void setLogFormatString(String format) {
        Logger.setFormatString(format);
    }

    public String getLogFormatString() {
        return Logger.getFormatString();
    }

    public void setLogTimestampFormat(String format) {
        Logger.setTimestampFormat(format);
    }

    public String getLogTimestampFormat() {
        return Logger.getTimestampFormat();
    }

    public void setLogMaxModuleNameLength(int maxLength) {
        Logger.setMaxModuleNameLength(maxLength);
    }

    public int getLogMaxModuleNameLength() {
        return Logger.getMaxModuleNameLength();
    }

    public void setLogForceModuleWidth(boolean force) {
        Logger.setForceModuleWidth(force);
    }

    public boolean isLogForceModuleWidth() {
        return Logger.isForceModuleWidth();
    }
}

