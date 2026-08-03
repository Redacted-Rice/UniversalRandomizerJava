package redactedrice.randomizer.lua;

import java.util.*;

import redactedrice.randomizer.utils.IssueTracker;

// Stores and indexes modules for efficient querying
// Provides lookup by id, group, modifies, and script type
public class ModuleRepository {
    // Modules are the core randomization that are manually specified and run
    // Map from module id to its metadata
    private final Map<String, Module> modules;
    // Modules organized by their group metadata field
    private final Map<String, List<Module>> modulesByGroup;
    // Modules organized by what they modify. Modules can be in more than one key/list here
    private final Map<String, List<Module>> modulesByModifies;
    // Scripts are automatically run before and after triggers
    private final Map<String, Map<String, List<Module>>> scriptsByType;
    // Scripts indexed by id for dependency resolution and validation
    private final Map<String, Module> scriptsById;
    // If set, this will restrict the groups that are loaded to only specified values. Null to
    // autodetermine from loading
    private final Set<String> definedGroups;
    // If set, this will restrict the modifies that are loaded to only specified values. Null to
    // autodetermine from loading
    private final Set<String> definedModifies;

    public static final String SCRIPT_TIMING_PRE = "pre";
    public static final String SCRIPT_TIMING_POST = "post";

    public static final String SCRIPT_WHEN_RANDOMIZE = "randomize";
    public static final String SCRIPT_WHEN_MODULE = "module";

    public ModuleRepository(Set<String> definedGroups, Set<String> definedModifies) {
        this.modules = new HashMap<>();
        this.modulesByGroup = new HashMap<>();
        this.modulesByModifies = new HashMap<>();
        this.scriptsByType = new HashMap<>();
        this.scriptsById = new HashMap<>();
        this.definedGroups = normalizeStringSet(definedGroups);
        this.definedModifies = normalizeStringSet(definedModifies);

        // Initialize the scripts maps
        Map<String, List<Module>> preScripts = new HashMap<>();
        preScripts.put(SCRIPT_WHEN_RANDOMIZE, new ArrayList<>());
        preScripts.put(SCRIPT_WHEN_MODULE, new ArrayList<>());
        scriptsByType.put(SCRIPT_TIMING_PRE, preScripts);

        Map<String, List<Module>> postScripts = new HashMap<>();
        postScripts.put(SCRIPT_WHEN_RANDOMIZE, new ArrayList<>());
        postScripts.put(SCRIPT_WHEN_MODULE, new ArrayList<>());
        scriptsByType.put(SCRIPT_TIMING_POST, postScripts);
    }

    // Register a module (not a script). Returns false if filtered out or id already taken.
    public boolean registerModule(Module module, ModuleFilter filter) {
        if (module == null) {
            return false;
        }

        if (!filter.accepts(module)) {
            return false;
        }

        if (!isIdAvailable(module.getId(), "module")) {
            return false;
        }

        modules.put(module.getId(), module);

        // Add to group indices
        addModuleToCategoryIndices(module, module.getGroups(), modulesByGroup, definedGroups);

        // Add to modifies indices
        addModuleToCategoryIndices(module, module.getModifies(), modulesByModifies,
                definedModifies);
        return true;
    }

    // Register a script (pre or post). Returns false if id already taken.
    public boolean registerScript(Module script, String timing) {
        if (script == null) {
            return false;
        }

        if (!isIdAvailable(script.getId(), "script")) {
            return false;
        }

        // Determine the when it should be run
        String when = script.getWhen();
        String whenKey;

        if (when != null && when.equals(SCRIPT_WHEN_MODULE)) {
            whenKey = SCRIPT_WHEN_MODULE;
        } else {
            // Default to randomize
            whenKey = SCRIPT_WHEN_RANDOMIZE;
        }

        scriptsById.put(script.getId(), script);
        scriptsByType.get(timing).get(whenKey).add(script);
        return true;
    }

    public boolean isIdAvailable(String id, String kind) {
        if (modules.containsKey(id) || scriptsById.containsKey(id)) {
            IssueTracker.addError("Duplicate " + kind + " id '" + id
                    + "': a module or script with this id is already registered");
            return false;
        }
        return true;
    }

    private void addModuleToCategoryIndices(Module module, Set<String> categories,
            Map<String, List<Module>> indexMap, Set<String> definedCategories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        for (String category : categories) {
            if (category != null && !category.trim().isEmpty()) {
                // Only add if not filtering or if in defined list
                if (definedCategories == null || definedCategories.isEmpty()
                        || definedCategories.contains(category)) {
                    indexMap.computeIfAbsent(category, k -> new ArrayList<>()).add(module);
                }
            }
        }
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

    // Query methods

    public Module getModule(String moduleId) {
        return modules.get(moduleId);
    }

    public Module getScript(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return null;
        }
        return scriptsById.get(moduleId);
    }

    public List<Module> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    public Set<String> getModuleIds() {
        return new HashSet<>(modules.keySet());
    }

    public Set<String> getDefinedGroupValues() {
        // Return defined groups if set. Otherwise return dynamically loaded values
        if (definedGroups != null && !definedGroups.isEmpty()) {
            return new HashSet<>(definedGroups);
        }
        return new HashSet<>(modulesByGroup.keySet());
    }

    public List<Module> getModulesByGroup(String group) {
        if (group == null || group.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Module> groupModules = modulesByGroup.get(group);
        return groupModules != null ? new ArrayList<>(groupModules) : new ArrayList<>();
    }

    public Set<String> getDefinedModifiesValues() {
        // Return defined modifies values if set. Otherwise return dynamically loaded values
        if (definedModifies != null && !definedModifies.isEmpty()) {
            return new HashSet<>(definedModifies);
        }
        return new HashSet<>(modulesByModifies.keySet());
    }

    public List<Module> getModulesByModifies(String modifies) {
        if (modifies == null || modifies.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Module> modifiesModules = modulesByModifies.get(modifies);
        return modifiesModules != null ? new ArrayList<>(modifiesModules) : new ArrayList<>();
    }

    public List<Module> getScripts(String timing, String when) {
        Map<String, List<Module>> timingMap = scriptsByType.get(timing);
        if (timingMap == null) {
            return new ArrayList<>();
        }

        List<Module> scripts = timingMap.get(when);
        return scripts != null ? new ArrayList<>(scripts) : new ArrayList<>();
    }

    public List<Module> getAllScripts(String timing) {
        Map<String, List<Module>> timingMap = scriptsByType.get(timing);
        if (timingMap == null) {
            return new ArrayList<>();
        }

        List<Module> scripts = new ArrayList<>();
        for (List<Module> whenScripts : timingMap.values()) {
            scripts.addAll(whenScripts);
        }
        scripts.sort(Comparator.comparing(Module::getName));
        return scripts;
    }

    public List<Module> getAllScripts() {
        return new ArrayList<>(scriptsById.values());
    }

    /**
     * Returns every registered action module and script for requirement validation.
     */
    public List<Module> getAllModulesAndScripts() {
        List<Module> loaded = new ArrayList<>(modules.values());
        loaded.addAll(scriptsById.values());
        return loaded;
    }

    public void clear() {
        modules.clear();
        modulesByGroup.clear();
        modulesByModifies.clear();
        scriptsById.clear();
        for (Map<String, List<Module>> timingMap : scriptsByType.values()) {
            for (List<Module> scripts : timingMap.values()) {
                scripts.clear();
            }
        }
    }
}
