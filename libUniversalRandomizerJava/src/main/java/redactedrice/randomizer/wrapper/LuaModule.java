package redactedrice.randomizer.wrapper;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import redactedrice.randomizer.metadata.ArgumentDefinition;

import java.io.File;
import java.util.*;

// holds metadata and execution function for a lua randomizer module
public class LuaModule {
    String name;
    String description;
    Set<String> groups;
    Set<String> modifies;
    List<ArgumentDefinition> arguments;
    LuaFunction executeFunction;
    LuaFunction onLoadFunction; // Optional onLoad function
    String filePath;
    int defaultSeedOffset;
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
    // Optional info fields
    String source;
    String license;
    String about;

    public LuaModule(String name, String description, Set<String> groups, Set<String> modifies,
            List<ArgumentDefinition> arguments, LuaFunction executeFunction,
            LuaFunction onLoadFunction, String filePath, int defaultSeedOffset, String when,
            String author, String version, Map<String, String> requires, String source,
            String license, String about) {
        // validate required fields
        validateRequiredFields(name, executeFunction, author, version, requires);

        // For regular modules (when == null) groups are required
        // Scripts (when != null) should not have groups or modifies
        boolean isScript = when != null && !when.trim().isEmpty();
        validateGroupsForModuleType(groups, isScript);
        validateModifiesForModuleType(modifies, isScript);

        // initialize all fields with defaults where appropriate
        this.name = name;
        this.description = description != null ? description : "";
        this.groups = normalizeStringSet(groups);
        this.modifies = normalizeStringSet(modifies);
        this.arguments = arguments != null ? new ArrayList<>(arguments) : new ArrayList<>();
        this.executeFunction = executeFunction;
        this.onLoadFunction = onLoadFunction; // can be null
        this.filePath = filePath;
        this.defaultSeedOffset = defaultSeedOffset;
        this.when = when;
        this.author = author;
        this.version = version;
        this.requires = requires != null ? new HashMap<>(requires) : new HashMap<>();
        this.source = source;
        this.license = license;
        this.about = about;
    }

    public static LuaModule parseFromFile(LuaTable moduleTable, File file) {
        String fileName = file.getName();

        String name = LuaParser.parseString(moduleTable, "name", null, fileName);
        String description = LuaParser.parseString(moduleTable, "description", "", fileName);
        Set<String> groups = LuaParser.parseStringSet(moduleTable, "groups", fileName);
        Set<String> modifies = LuaParser.parseStringSet(moduleTable, "modifies", fileName);

        Integer seedOffsetInt = LuaParser.parseInt(moduleTable, "seedOffset", fileName);
        // Default to 0
        int seedOffset = (seedOffsetInt != null) ? seedOffsetInt : 0;
        LuaFunction executeFunction = LuaParser.parseFunction(moduleTable, "execute", fileName);
        LuaFunction onLoadFunction = LuaParser.parseFunction(moduleTable, "onLoad", fileName);

        // Parse arguments - handled separately due to complexity
        List<ArgumentDefinition> arguments = redactedrice.randomizer.metadata.ArgumentParser
                .parseArgumentsFromTable(moduleTable, fileName);

        String when = LuaParser.parseString(moduleTable, "when", null, fileName);
        String author = LuaParser.parseString(moduleTable, "author", null, fileName);
        String version = LuaParser.parseString(moduleTable, "version", null, fileName);
        Map<String, String> requires = LuaParser.parseStringMap(moduleTable, "requires", fileName);
        String source = LuaParser.parseString(moduleTable, "source", null, fileName);
        String license = LuaParser.parseString(moduleTable, "license", null, fileName);
        String about = LuaParser.parseString(moduleTable, "about", null, fileName);

        // Create the module. This will validate and throw if there are issues
        try {
            return new LuaModule(name, description, groups, modifies, arguments, executeFunction,
                    onLoadFunction, file.getAbsolutePath(), seedOffset, when, author, version,
                    requires, source, license, about);
        } catch (IllegalArgumentException e) {
            redactedrice.randomizer.logger.ErrorTracker
                    .addError(fileName + " validation failed: " + e.getMessage());
            return null;
        }
    }

    private void validateRequiredFields(String name, LuaFunction executeFunction, String author,
            String version, Map<String, String> requires) {
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
        if (requires == null || requires.isEmpty()) {
            throw new IllegalArgumentException("Requires cannot be null or empty");
        }
        if (!requires.containsKey("UniversalRandomizerJava")) {
            throw new IllegalArgumentException(
                    "Requires must specify the UniversalRandomizerJava version");
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
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                normalized.add(value.toLowerCase());
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    // Getters
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

    public int getDefaultSeedOffset() {
        return defaultSeedOffset;
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
                "LuaModule{name='%s', groups=%s, modifies=%s, description='%s', arguments=%d, "
                        + "seedOffset=%d, when='%s', filePath='%s', author='%s', version='%s'}",
                name, groups, modifies, description, arguments.size(), defaultSeedOffset, when,
                filePath, author, version);
    }
}
