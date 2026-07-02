package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;
import redactedrice.randomizer.lua.arguments.ArgumentParser;
import redactedrice.randomizer.utils.ErrorTracker;
import redactedrice.randomizer.utils.LuaJavaConverter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Parses a Lua table into a Module object
// Extracts metadata and validates structure
public class ModuleParser {
    private static final int NAME_HASH_OFFSET_MAX = 9999;

    private ModuleParser() {}

    public static int hashNameToSeedOffset(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return 1;
        }
        return Math.floorMod(moduleName.hashCode(), NAME_HASH_OFFSET_MAX) + 1;
    }

    // Parse a Lua table into a Module object
    public static Module parse(LuaTable moduleTable, Path sourceFile) {
        if (moduleTable == null) {
            throw new IllegalArgumentException("Module table cannot be null");
        }
        if (sourceFile == null) {
            throw new IllegalArgumentException("Source file cannot be null");
        }

        String fileName = sourceFile.getFileName().toString();

        // Extract all fields from the Lua table
        String name = LuaJavaConverter.tryGetStringFromTable(moduleTable, "name", null, fileName);
        String description =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "description", "", fileName);
        Set<String> groups =
                LuaJavaConverter.tryGetStringSetFromTable(moduleTable, "groups", fileName);
        Set<String> modifies =
                LuaJavaConverter.tryGetStringSetFromTable(moduleTable, "modifies", fileName);
        String when = LuaJavaConverter.tryGetStringFromTable(moduleTable, "when", null, fileName);
        boolean isScript = when != null && !when.trim().isEmpty();

        boolean seeded = false;
        int seedOffset = 0;
        boolean seedOffsetFromMetadata = false;

        if (isScript) {
            // Treat these as non-fatal for the script
            if (!moduleTable.get("defaultSeedOffset").isnil()) {
                ErrorTracker.addError(
                        fileName + " field 'defaultSeedOffset' is not allowed on scripts");
            }
            if (!moduleTable.get("seeded").isnil()) {
                ErrorTracker.addError(fileName + " field 'seeded' is not allowed on scripts");
            }
        } else {
            Boolean parsedSeeded =
                    LuaJavaConverter.tryGetBooleanFromTable(moduleTable, "seeded", fileName, true);
            seeded = parsedSeeded != null ? parsedSeeded : true;
            if (seeded) {
                Integer metadataSeedOffset = LuaJavaConverter.tryGetIntFromTable(moduleTable,
                        "defaultSeedOffset", fileName);
                if (metadataSeedOffset != null) {
                    seedOffset = metadataSeedOffset;
                    seedOffsetFromMetadata = true;
                } else {
                    seedOffset = hashNameToSeedOffset(name);
                }
            }
        }

        LuaFunction executeFunction =
                LuaJavaConverter.tryGetFunctionFromTable(moduleTable, "execute", fileName);
        LuaFunction onLoadFunction =
                LuaJavaConverter.tryGetFunctionFromTable(moduleTable, "onLoad", fileName);

        // Parse arguments - handled separately due to complexity
        List<ArgumentDefinition> arguments =
                ArgumentParser.parseArgumentsFromTable(moduleTable, fileName);

        String author =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "author", null, fileName);
        String version =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "version", null, fileName);
        Map<String, String> requires =
                LuaJavaConverter.tryGetStringMapFromTable(moduleTable, "requires", fileName);
        String source =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "source", null, fileName);
        String license =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "license", null, fileName);
        String about = LuaJavaConverter.tryGetStringFromTable(moduleTable, "about", null, fileName);

        // Create the module. This will validate and throw if there are issues
        try {
            return new Module(name, description, groups, modifies, arguments, executeFunction,
                    onLoadFunction, sourceFile.toAbsolutePath().toString(), seedOffset,
                    seedOffsetFromMetadata, seeded, when, author, version, requires, source,
                    license, about);
        } catch (IllegalArgumentException e) {
            ErrorTracker.addError(fileName + " validation failed: " + e.getMessage());
            return null;
        }
    }
}
