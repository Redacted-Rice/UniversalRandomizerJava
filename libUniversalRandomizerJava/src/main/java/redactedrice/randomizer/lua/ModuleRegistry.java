package redactedrice.randomizer.lua;

import redactedrice.randomizer.utils.Logger;
import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.randomizer.lua.requirements.CoreRequirements;
import redactedrice.randomizer.lua.requirements.RequirementIssue;
import redactedrice.randomizer.lua.requirements.RequirementValidator;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

// loads lua modules from directories, parses their metadata, and stores them providing lookup
// mechanisms for them/their metadata
public class ModuleRegistry {
    private final ModuleLoader loader;
    private final ModuleRepository repository;
    private final ModuleFilter moduleFilter;
    private final CoreRequirements requirementContext;

    public static final String SCRIPT_TIMING_PRE = ModuleRepository.SCRIPT_TIMING_PRE;
    public static final String SCRIPT_TIMING_POST = ModuleRepository.SCRIPT_TIMING_POST;

    public static final String SCRIPT_WHEN_RANDOMIZE = ModuleRepository.SCRIPT_WHEN_RANDOMIZE;
    public static final String SCRIPT_WHEN_MODULE = ModuleRepository.SCRIPT_WHEN_MODULE;

    public ModuleRegistry(LuaSandbox sandbox) {
        this(sandbox, null, null, null);
    }

    public ModuleRegistry(LuaSandbox sandbox, Set<String> definedGroups,
            Set<String> definedModifies) {
        this(sandbox, definedGroups, definedModifies, null);
    }

    public ModuleRegistry(LuaSandbox sandbox, Set<String> definedGroups,
            Set<String> definedModifies, CoreRequirements requirementContext) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.loader = new ModuleLoader(sandbox);
        this.repository = new ModuleRepository(definedGroups, definedModifies);
        this.moduleFilter = new CompositeFilter(new GroupFilter(definedGroups),
                new ModifiesFilter(definedModifies));
        this.requirementContext = requirementContext;
    }

    public int loadModulesFromDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            IssueTracker.addError("Directory path cannot be null or empty");
            return 0;
        }

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            IssueTracker.addError("Directory does not exist: " + directoryPath);
            return 0;
        }

        if (!directory.isDirectory()) {
            IssueTracker.addError("Path is not a directory: " + directoryPath);
            return 0;
        }

        // Load modules from actions subfolder
        int loadedCount = loadModulesFromSubfolder(directoryPath);

        // Load the pre & post scripts as well
        loadPreScriptsFromDirectory(directoryPath);
        loadPostScriptsFromDirectory(directoryPath);
        return loadedCount;
    }

    private List<Path> getScriptsFromSubdirectory(String directoryPath, String subfolder) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Path targetDir;
        if (subfolder == null || subfolder.trim().isEmpty()) {
            targetDir = new File(directoryPath).toPath();
        } else {
            targetDir = new File(directoryPath, subfolder).toPath();
        }

        return loader.findLuaFiles(targetDir);
    }

    private int loadModulesFromSubfolder(String directoryPath) {
        List<Path> luaFiles = getScriptsFromSubdirectory(directoryPath, "actions");
        int loadedCount = 0;

        for (Path file : luaFiles) {
            try {
                Module module = loadAndParseModule(file);
                if (module != null && repository.registerModule(module, moduleFilter)) {
                    loadedCount++;
                    Logger.info("Loaded from module: " + module.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                IssueTracker.addError("Error loading script from " + file + ": " + e.getMessage());
            }
        }

        return loadedCount;
    }

    private int loadScriptsFromSubfolder(String directoryPath, String subfolder, String timing) {
        List<Path> luaFiles = getScriptsFromSubdirectory(directoryPath, subfolder);
        int loadedCount = 0;

        for (Path file : luaFiles) {
            try {
                Module script = loadAndParseModule(file);
                if (script != null && repository.registerScript(script, timing)) {
                    loadedCount++;
                    Logger.info("Loaded from script: " + script.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                IssueTracker.addError("Error loading script from " + file + ": " + e.getMessage());
            }
        }

        return loadedCount;
    }

    public int loadPreScriptsFromDirectory(String directoryPath) {
        return loadScriptsFromSubfolder(directoryPath, "prescripts", SCRIPT_TIMING_PRE);
    }

    public int loadPostScriptsFromDirectory(String directoryPath) {
        return loadScriptsFromSubfolder(directoryPath, "postscripts", SCRIPT_TIMING_POST);
    }

    private Module loadAndParseModule(Path filePath) {
        Logger.info("Loading module: " + filePath.getFileName());
        LuaValue luaTable = loader.loadFile(filePath);
        if (luaTable == null || !luaTable.istable()) {
            return null;
        }
        return ModuleParser.parse(luaTable.checktable(), filePath);
    }


    public Module getModule(String moduleId) {
        return repository.getModule(moduleId);
    }

    public Module getScript(String moduleId) {
        return repository.getScript(moduleId);
    }

    public Set<String> getDefinedGroupValues() {
        return repository.getDefinedGroupValues();
    }

    public List<Module> getModulesByGroup(String group) {
        return repository.getModulesByGroup(group);
    }

    public Set<String> getDefinedModifiesValues() {
        return repository.getDefinedModifiesValues();
    }

    public List<Module> getModulesByModifies(String modifies) {
        return repository.getModulesByModifies(modifies);
    }

    public List<Module> getAllModules() {
        return repository.getAllModules();
    }

    public Set<String> getModuleIds() {
        return repository.getModuleIds();
    }

    public List<Module> getScripts(String timing, String when) {
        return repository.getScripts(timing, when);
    }

    public List<Module> getAllScripts(String timing) {
        return repository.getAllScripts(timing);
    }

    public List<RequirementIssue> validateAllRequirements() {
        List<RequirementIssue> issues =
                RequirementValidator.validate(requirementContext, repository);
        for (RequirementIssue issue : issues) {
            if (issue.isError()) {
                IssueTracker.addError("module requirements", issue.getMessage());
            } else {
                IssueTracker.addWarning("module requirements", issue.getMessage());
            }
        }
        return issues;
    }

    public void clear() {
        repository.clear();
        IssueTracker.clear();
    }
}
