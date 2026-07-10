package redactedrice.randomizer.lua;

import java.util.*;

// Stores and indexes modules for efficient querying
// Provides lookup by name, group, modifies, and script type
public class ModuleRepository {
    // Modules are the core randomization that are manually specified and run
    // This is a map from the module name to its metadata
    private final Map<String, Module> modules;
    // Modules organized by their group metadata field
    private final Map<String, List<Module>> modulesByGroup;
    // Modules organized by what they modify. Modules can be in more than one key/list here
    private final Map<String, List<Module>> modulesByModifies;
    // Scripts are automatically run before and after triggers. Name may change
    private final Map<String, Map<String, List<Module>>> scriptsByType;
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

    // Register a module (not a script)
    public void registerModule(Module module, ModuleFilter filter) {
        if (module == null) {
            return;
        }

        if (!filter.accepts(module)) {
            return;
        }

        modules.put(module.getName(), module);

        // Add to group indices
        addModuleToCategoryIndices(module, module.getGroups(), modulesByGroup, definedGroups);

        // Add to modifies indices
        addModuleToCategoryIndices(module, module.getModifies(), modulesByModifies,
                definedModifies);
    }

    // Register a script (pre or post)
    public void registerScript(Module script, String timing) {
        if (script == null) {
            return;
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

        // Add to the appropriate list in the nested map
        scriptsByType.get(timing).get(whenKey).add(script);
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

    public Module getModule(String name) {
        return modules.get(name);
    }

    public Module getScript(String name) {
        return findScriptByName(name);
    }

    private Module findScriptByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (Map<String, List<Module>> timingMap : scriptsByType.values()) {
            for (List<Module> scripts : timingMap.values()) {
                for (Module script : scripts) {
                    if (name.equals(script.getName())) {
                        return script;
                    }
                }
            }
        }
        return null;
    }

    public List<Module> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    public Set<String> getModuleNames() {
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

    public void clear() {
        modules.clear();
        modulesByGroup.clear();
        modulesByModifies.clear();
        for (Map<String, List<Module>> timingMap : scriptsByType.values()) {
            for (List<Module> scripts : timingMap.values()) {
                scripts.clear();
            }
        }
    }
}
