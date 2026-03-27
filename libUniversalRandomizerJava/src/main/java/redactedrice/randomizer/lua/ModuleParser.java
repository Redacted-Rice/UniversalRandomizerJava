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

        Integer seedOffsetInt =
                LuaJavaConverter.tryGetIntFromTable(moduleTable, "seedOffset", fileName);
        // Default to 0
        int seedOffset = (seedOffsetInt != null) ? seedOffsetInt : 0;
        LuaFunction executeFunction =
                LuaJavaConverter.tryGetFunctionFromTable(moduleTable, "execute", fileName);
        LuaFunction onLoadFunction =
                LuaJavaConverter.tryGetFunctionFromTable(moduleTable, "onLoad", fileName);

        // Parse arguments - handled separately due to complexity
        List<ArgumentDefinition> arguments =
                ArgumentParser.parseArgumentsFromTable(moduleTable, fileName);

        String when = LuaJavaConverter.tryGetStringFromTable(moduleTable, "when", null, fileName);
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
        String about =
                LuaJavaConverter.tryGetStringFromTable(moduleTable, "about", null, fileName);

        // Create the module. This will validate and throw if there are issues
        try {
            return new Module(name, description, groups, modifies, arguments, executeFunction,
                    onLoadFunction, sourceFile.toAbsolutePath().toString(), seedOffset, when,
                    author, version, requires, source, license, about);
        } catch (IllegalArgumentException e) {
            ErrorTracker.addError(fileName + " validation failed: " + e.getMessage());
            return null;
        }
    }
}
