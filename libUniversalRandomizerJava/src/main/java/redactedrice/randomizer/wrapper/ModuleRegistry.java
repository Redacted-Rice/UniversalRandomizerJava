package redactedrice.randomizer.wrapper;

import redactedrice.randomizer.logger.Logger;
import redactedrice.randomizer.logger.ErrorTracker;
import redactedrice.randomizer.wrapper.sandbox.LuaSandbox;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.*;

// loads lua modules from directories, parses their metadata, and stores them providing lookup
// mechanisms for them/their metadata
public class ModuleRegistry {
    LuaSandbox sandbox;
    // Modules are the core randomization that are manually specified and run
    // This is a map from the module name to its metadata
    Map<String, LuaModule> modules;
    // Modules organized by their group metadata field
    Map<String, List<LuaModule>> modulesByGroup;
    // Modules organized by what they modify. Modules can be in more than one key/list here
    Map<String, List<LuaModule>> modulesByModifies;
    // Scripts are automatically run before and after triggers. Name may change
    Map<String, Map<String, List<LuaModule>>> scriptsByType;
    // If set, this will restrict the groups that are loaded to only specified values. Null to
    // autodetermine from loading
    Set<String> definedGroups;
    // If set, this will restrict the modifies that are loaded to only specified values. Null to
    // autodetermine from loading
    Set<String> definedModifies;

    public static final String SCRIPT_TIMING_PRE = "pre";
    public static final String SCRIPT_TIMING_POST = "post";

    public static final String SCRIPT_WHEN_RANDOMIZE = "randomize";
    public static final String SCRIPT_WHEN_MODULE = "module";

    public ModuleRegistry(LuaSandbox sandbox) {
        this(sandbox, null, null);
    }

    public ModuleRegistry(LuaSandbox sandbox, Set<String> definedGroups,
            Set<String> definedModifies) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
        this.modules = new HashMap<>();
        this.modulesByGroup = new HashMap<>();
        this.modulesByModifies = new HashMap<>();
        this.scriptsByType = new HashMap<>();
        this.definedGroups = normalizeStringSet(definedGroups);
        this.definedModifies = normalizeStringSet(definedModifies);

        // Initialize the scripts maps
        Map<String, List<LuaModule>> preScripts = new HashMap<>();
        preScripts.put(SCRIPT_WHEN_RANDOMIZE, new ArrayList<>());
        preScripts.put(SCRIPT_WHEN_MODULE, new ArrayList<>());
        scriptsByType.put(SCRIPT_TIMING_PRE, preScripts);

        Map<String, List<LuaModule>> postScripts = new HashMap<>();
        postScripts.put(SCRIPT_WHEN_RANDOMIZE, new ArrayList<>());
        postScripts.put(SCRIPT_WHEN_MODULE, new ArrayList<>());
        scriptsByType.put(SCRIPT_TIMING_POST, postScripts);
    }

    public int loadModulesFromDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            ErrorTracker.addError("Directory path cannot be null or empty");
            return 0;
        }

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            ErrorTracker.addError("Directory does not exist: " + directoryPath);
            return 0;
        }

        if (!directory.isDirectory()) {
            ErrorTracker.addError("Path is not a directory: " + directoryPath);
            return 0;
        }

        // Load modules from actions subfolder
        int loadedCount = loadModulesFromSubfolder(directoryPath);

        // Load the pre & post scripts as well
        loadPreScriptsFromDirectory(directoryPath);
        loadPostScriptsFromDirectory(directoryPath);

        return loadedCount;
    }


    private List<File> getScriptsFromSubdirectory(String directoryPath, String subfolder) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return new ArrayList<>();
        }

        File targetDir;
        if (subfolder == null || subfolder.trim().isEmpty()) {
            targetDir = new File(directoryPath);
        } else {
            targetDir = new File(directoryPath, subfolder);
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return new ArrayList<>();
        }

        return findLuaFiles(targetDir);
    }

    private int loadModulesFromSubfolder(String directoryPath) {
        List<File> luaFiles = getScriptsFromSubdirectory(directoryPath, "actions");
        return loadModulesFromScripts(luaFiles, modules, "module", (metadata) -> {
            if (!isAllowedGrouping(metadata.getGroups(), definedGroups, metadata.getName(),
                    "groups", "group")) {
                return;
            } else if (!isAllowedGrouping(metadata.getModifies(), definedModifies,
                    metadata.getName(), "modifies", "modifies")) {
                return;
            }

            modules.put(metadata.getName(), metadata);

            // Add to group indices
            addModuleToCategoryIndices(metadata, metadata.getGroups(), modulesByGroup,
                    definedGroups);

            // Add to modifies indices
            addModuleToCategoryIndices(metadata, metadata.getModifies(), modulesByModifies,
                    definedModifies);
        });
    }

    private boolean isAllowedGrouping(Set<String> metadataValues, Set<String> definedValues,
            String moduleName, String fieldName, String singularName) {
        if (definedValues == null || definedValues.isEmpty()) {
            return true;
        }

        boolean hasMatch = false;
        if (metadataValues != null && !metadataValues.isEmpty()) {
            for (String value : metadataValues) {
                if (value != null && !value.trim().isEmpty()) {
                    if (definedValues.contains(value)) {
                        hasMatch = true;
                    } else {
                        Logger.warn("Module '" + moduleName + "' has " + singularName + " '" + value
                                + "' which is not in defined " + fieldName + " values");
                    }
                }
            }
        }
        if (!hasMatch) {
            Logger.warn("Ignoring module '" + moduleName + "' - no " + fieldName
                    + " values in defined list");
        }
        return hasMatch;
    }

    private void addModuleToCategoryIndices(LuaModule metadata, Set<String> categories,
            Map<String, List<LuaModule>> indexMap, Set<String> definedCategories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        for (String category : categories) {
            if (category != null && !category.trim().isEmpty()) {
                // Only add if not filtering or if in defined list
                if (definedCategories == null || definedCategories.isEmpty()
                        || definedCategories.contains(category)) {
                    indexMap.computeIfAbsent(category, k -> new ArrayList<>()).add(metadata);
                }
            }
        }
    }

    private int loadScriptsFromSubfolder(String directoryPath, String subfolder, String timing) {
        List<File> luaFiles = getScriptsFromSubdirectory(directoryPath, subfolder);
        return loadModulesFromScripts(luaFiles, null, directoryPath, (metadata) -> {
            // Determine the when it should be run
            String when = metadata.getWhen();
            String whenKey;

            if (when != null && when.equals(SCRIPT_WHEN_MODULE)) {
                whenKey = SCRIPT_WHEN_MODULE;
            } else {
                // Default to randomize
                whenKey = SCRIPT_WHEN_RANDOMIZE;
            }

            // Add to the appropriate list in the nested map
            scriptsByType.get(timing).get(whenKey).add(metadata);
        });
    }

    private int loadModulesFromScripts(List<File> luaFiles, Object targetCollection,
            String scriptType, java.util.function.Consumer<LuaModule> onSuccess) {
        int loadedCount = 0;

        for (File file : luaFiles) {
            try {
                LuaModule metadata = loadModule(file);
                if (metadata != null) {
                    onSuccess.accept(metadata);
                    loadedCount++;
                    Logger.info("Loaded from " + scriptType + ": " + metadata.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                ErrorTracker.addError(
                        "Error loading script from " + file.getPath() + ": " + e.getMessage());
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

    private LuaModule loadModule(File file) {
        Logger.info("Loading module: " + file.getName());
        try {
            LuaValue result = sandbox.executeFile(file.getAbsolutePath());
            if (!result.istable()) {
                ErrorTracker.addError(
                        file.getName() + " did not return a table (got " + result.typename() + ")");
                return null;
            }
            return LuaModule.parseFromFile(result.checktable(), file);
        } catch (LuaError e) {
            ErrorTracker.addError("Lua error in " + file.getName() + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            ErrorTracker.addError("Error loading " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private List<File> findLuaFiles(File directory) {
        List<File> luaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // recurse into subdirectories
                    luaFiles.addAll(findLuaFiles(file));
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".lua")) {
                    luaFiles.add(file);
                }
            }
        }

        return luaFiles;
    }


    public LuaModule getModule(String name) {
        return modules.get(name);
    }

    public Set<String> getDefinedGroupValues() {
        // Return defined groups if set. Otherwise return dynamically loaded values
        if (definedGroups != null && !definedGroups.isEmpty()) {
            return new HashSet<>(definedGroups);
        }
        return new HashSet<>(modulesByGroup.keySet());
    }

    public List<LuaModule> getModulesByGroup(String group) {
        if (group == null || group.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<LuaModule> groupModules = modulesByGroup.get(group);
        return groupModules != null ? new ArrayList<>(groupModules) : new ArrayList<>();
    }

    public Set<String> getDefinedModifiesValues() {
        // Return defined modifies values if set. Otherwise return dynamically loaded values
        if (definedModifies != null && !definedModifies.isEmpty()) {
            return new HashSet<>(definedModifies);
        }
        return new HashSet<>(modulesByModifies.keySet());
    }

    public List<LuaModule> getModulesByModifies(String modifies) {
        if (modifies == null || modifies.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<LuaModule> modifiesModules = modulesByModifies.get(modifies);
        return modifiesModules != null ? new ArrayList<>(modifiesModules) : new ArrayList<>();
    }

    private Set<String> normalizeStringSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }

    public List<LuaModule> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    public Set<String> getModuleNames() {
        return new HashSet<>(modules.keySet());
    }

    public List<LuaModule> getScripts(String timing, String when) {
        Map<String, List<LuaModule>> timingMap = scriptsByType.get(timing);
        if (timingMap == null) {
            return new ArrayList<>();
        }

        List<LuaModule> scripts = timingMap.get(when);
        return scripts != null ? new ArrayList<>(scripts) : new ArrayList<>();
    }

    public void clear() {
        modules.clear();
        modulesByGroup.clear();
        modulesByModifies.clear();
        for (Map<String, List<LuaModule>> timingMap : scriptsByType.values()) {
            for (List<LuaModule> scripts : timingMap.values()) {
                scripts.clear();
            }
        }
        // TODO: Handle this better - probably means making non static
        ErrorTracker.clearErrors();
    }
}
