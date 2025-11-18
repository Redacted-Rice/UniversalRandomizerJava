package redactedrice.randomizer.metadata;

import org.luaj.vm2.LuaFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// holds metadata and execution function for a lua randomizer module
public class LuaModuleMetadata {
    String name;
    String description;
    String group;
    List<String> modifies;
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

    public LuaModuleMetadata(String name, String description, String group, List<String> modifies,
            List<ArgumentDefinition> arguments, LuaFunction executeFunction,
            LuaFunction onLoadFunction, String filePath, int defaultSeedOffset, String when,
            String author, String version, Map<String, String> requires, String source,
            String license, String about) {
        // validate required fields
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

        // initialize all fields with defaults where appropriate
        this.name = name;
        this.description = description != null ? description : "";
        this.group = group != null ? group.toLowerCase() : "utility"; // default group is utility
        this.modifies = modifies != null ? new ArrayList<>(modifies) : new ArrayList<>();
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

        // note: pseudo-enum registration is now handled by lua randomization wrapper
        // after loading
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getGroup() {
        return group;
    }

    public List<String> getModifies() {
        return Collections.unmodifiableList(modifies);
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
                "LuaModuleMetadata{name='%s', group='%s', modifies=%s, description='%s', arguments=%d, seedOffset=%d, when='%s', filePath='%s', author='%s', version='%s'}",
                name, group, modifies, description, arguments.size(), defaultSeedOffset, when,
                filePath, author, version);
    }
}
