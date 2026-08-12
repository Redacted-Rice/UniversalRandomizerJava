package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaFunction;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;

import redactedrice.randomizer.lua.dynamicVar.DynamicVar;

import java.util.*;

// holds metadata and execution function for a lua randomizer module
public class Module {
    String id;
    String name;
    String description;
    Set<String> groups;
    Set<String> modifies;
    List<ArgumentDefinition> arguments;
    LuaFunction executeFunction;
    LuaFunction onLoadFunction; // Optional onLoad function
    String filePath;
    int seedOffset;
    boolean seedOffsetFromMetadata;
    // Whether this module participates in seed configuration (true for modules, false for scripts)
    boolean seeded;
    // When to execute: each randomization or for each module or null for regular
    // modules
    // Currently I call these "scripts" (run automatically before & after triggers)
    // vs "modules"
    // (run only when manual specified)
    // TODO: I think I want to move this to a seraprate class in the future
    String when;

    String author;
    String version;
    Map<String, String> requires;
    List<DynamicVar> provides;
    List<DynamicVar> needs;
    // Optional info fields
    String source;
    String license;
    String about;

    public Module(String id, String name, String description, Set<String> groups,
            Set<String> modifies, List<ArgumentDefinition> arguments, LuaFunction executeFunction,
            LuaFunction onLoadFunction, String filePath, int seedOffset,
            boolean seedOffsetFromMetadata, boolean seeded, String when, String author,
            String version, Map<String, String> requires, List<DynamicVar> provides,
            List<DynamicVar> needs, String source, String license, String about) {
        // validate required fields
        validateRequiredFields(id, name, executeFunction, author, version);

        // For regular modules (when == null) groups are required
        // Scripts (when != null) should not have groups or modifies
        boolean isScript = when != null && !when.trim().isEmpty();
        validateGroupsForModuleType(groups, isScript);
        validateModifiesForModuleType(modifies, isScript);

        // initialize all fields with defaults where appropriate
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.groups = normalizeStringSet(groups);
        this.modifies = normalizeStringSet(modifies);
        this.arguments = arguments != null ? new ArrayList<>(arguments) : new ArrayList<>();
        this.executeFunction = executeFunction;
        this.onLoadFunction = onLoadFunction; // can be null
        this.filePath = filePath;
        this.seedOffset = seedOffset;
        this.seedOffsetFromMetadata = seedOffsetFromMetadata;
        this.seeded = seeded;
        this.when = when;
        this.author = author;
        this.version = version;
        this.requires = requires != null ? new HashMap<>(requires) : new HashMap<>();
        this.provides = provides != null ? List.copyOf(provides) : List.of();
        this.needs = needs != null ? List.copyOf(needs) : List.of();
        this.source = source;
        this.license = license;
        this.about = about;
    }

    private void validateRequiredFields(String id, String name, LuaFunction executeFunction,
            String author, String version) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Module id cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        if (executeFunction == null) {
            throw new IllegalArgumentException("Execute function cannot be null");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be null or empty");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
    }

    private void validateGroupsForModuleType(Set<String> groups, boolean isScript) {
        if (!isScript) {
            // Regular modules require at least one group
            if (groups == null || groups.isEmpty()) {
                throw new IllegalArgumentException(
                        "Groups cannot be null or empty for regular modules");
            }
        } else {
            // Scripts should not have groups
            if (groups != null && !groups.isEmpty()) {
                throw new IllegalArgumentException("Scripts (when != null) should not have groups");
            }
        }
    }

    private void validateModifiesForModuleType(Set<String> modifies, boolean isScript) {
        if (isScript) {
            // Scripts should not have modifies
            if (modifies != null && !modifies.isEmpty()) {
                throw new IllegalArgumentException(
                        "Scripts (when != null) should not have modifies");
            }
        }
        // Modifies is optional for modules
    }

    private Set<String> normalizeStringSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        Map<String, String> byLowerKey = new LinkedHashMap<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String trimmed = value.trim();
                byLowerKey.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(byLowerKey.values()));
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public Set<String> getModifies() {
        return Collections.unmodifiableSet(modifies);
    }

    public List<ArgumentDefinition> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public LuaFunction getExecuteFunction() {
        return executeFunction;
    }

    public LuaFunction getOnLoadFunction() {
        return onLoadFunction;
    }

    public boolean hasOnLoad() {
        return onLoadFunction != null;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getSeedOffset() {
        return seedOffset;
    }

    public boolean isSeedOffsetFromMetadata() {
        return seedOffsetFromMetadata;
    }

    public boolean isSeeded() {
        return seeded;
    }

    public String getWhen() {
        return when;
    }

    public boolean isScript() {
        return when != null && !when.isEmpty();
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getRequires() {
        return Collections.unmodifiableMap(requires);
    }

    public List<DynamicVar> getProvides() {
        return Collections.unmodifiableList(provides);
    }

    public List<DynamicVar> getNeeds() {
        return Collections.unmodifiableList(needs);
    }

    public String getSource() {
        return source;
    }

    public String getLicense() {
        return license;
    }

    public String getAbout() {
        return about;
    }

    @Override
    public String toString() {
        return String.format(
                "Module{id='%s', name='%s', groups=%s, modifies=%s, description='%s', arguments=%d, "
                        + "seedOffset=%d, seedOffsetFromMetadata=%s, seeded=%s, when='%s', filePath='%s', author='%s', version='%s'}",
                id, name, groups, modifies, description, arguments.size(), seedOffset,
                seedOffsetFromMetadata, seeded, when, filePath, author, version);
    }
}
