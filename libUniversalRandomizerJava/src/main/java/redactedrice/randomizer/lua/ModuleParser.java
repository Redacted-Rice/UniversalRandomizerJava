package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;
import redactedrice.randomizer.lua.arguments.ArgumentParser;
import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.randomizer.utils.LuaJavaConverter;

import redactedrice.randomizer.lua.dynamicVar.DynamicVar;
import redactedrice.randomizer.lua.dynamicVar.DynamicVarParser;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Parses a Lua table into a Module object
// Extracts metadata and validates structure
public class ModuleParser {
    private static final int ID_HASH_OFFSET_MAX = 9999;

    private ModuleParser() {}

    /** Default seed offset derived from the module id */
    public static int hashIdToSeedOffset(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) {
            return 1;
        }
        return Math.floorMod(moduleId.hashCode(), ID_HASH_OFFSET_MAX) + 1;
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
        String id = LuaJavaConverter.tryGetStringFromTable(moduleTable, "id", null, fileName);
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
                IssueTracker.addError(
                        fileName + " field 'defaultSeedOffset' is not allowed on scripts");
            }
            if (!moduleTable.get("seeded").isnil()) {
                IssueTracker.addError(fileName + " field 'seeded' is not allowed on scripts");
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
                    seedOffset = hashIdToSeedOffset(id);
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
        List<DynamicVar> provides =
                DynamicVarParser.parseFromTable(moduleTable, "provides", fileName);
        List<DynamicVar> needs = DynamicVarParser.parseFromTable(moduleTable, "needs", fileName);
        if (IssueTracker.hasErrors()) {
            return null;
        }
        String source =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "source", null, fileName);
        String license =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "license", null, fileName);
        String about = LuaJavaConverter.tryGetStringFromTable(moduleTable, "about", null, fileName);

        // Create the module. This will validate and throw if there are issues
        try {
            return new Module(id, name, description, groups, modifies, arguments, executeFunction,
                    onLoadFunction, sourceFile.toAbsolutePath().toString(), seedOffset,
                    seedOffsetFromMetadata, seeded, when, author, version, requires, provides,
                    needs, source, license, about);
        } catch (IllegalArgumentException e) {
            IssueTracker.addError(fileName + " validation failed: " + e.getMessage());
            return null;
        }
    }
}
