package redactedrice.randomizer.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.utils.ErrorTracker;

class ModuleParserSeedMetadataTest {

    @TempDir
    Path tempDir;

    @Test
    void scriptsDefaultToUnseededWithoutSeedMetadata() throws IOException {
        Path luaFile = writeScript("""
                return {
                    id = "seedless_script",
                    name = "Seedless Script",
                    description = "test",
                    when = "randomize",
                    author = "Test",
                    version = "0.1",
                    execute = function(context) end,
                }
                """);

        Module module = parse(luaFile);
        assertNotNull(module);
        assertTrue(module.isScript());
        assertFalse(module.isSeeded());
        assertEquals(0, module.getSeedOffset());
        assertFalse(module.isSeedOffsetFromMetadata());
    }

    @Test
    void scriptsLogErrorForDefaultSeedOffsetButStillLoad() throws IOException {
        Path luaFile = writeScript("""
                return {
                    id = "bad_script",
                    name = "Bad Script",
                    when = "randomize",
                    author = "Test",
                    version = "0.1",
                    defaultSeedOffset = 10,
                    execute = function(context) end,
                }
                """);

        Module module = parse(luaFile);
        assertNotNull(module);
        assertTrue(module.isScript());
        assertFalse(module.isSeeded());
        assertEquals(0, module.getSeedOffset());
        assertTrue(ErrorTracker.hasErrors());
    }

    @Test
    void scriptsLogErrorForSeededFieldButStillLoad() throws IOException {
        Path luaFile = writeScript("""
                return {
                    id = "bad_script_seeded",
                    name = "Bad Script",
                    when = "randomize",
                    author = "Test",
                    version = "0.1",
                    seeded = true,
                    execute = function(context) end,
                }
                """);

        Module module = parse(luaFile);
        assertNotNull(module);
        assertTrue(module.isScript());
        assertFalse(module.isSeeded());
        assertEquals(0, module.getSeedOffset());
        assertTrue(ErrorTracker.hasErrors());
    }

    @Test
    void modulesDefaultToSeededAndUseNameHashOffset() throws IOException {
        Path luaFile = writeScript("""
                return {
                    id = "seeded_module",
                    name = "Seeded Module",
                    groups = { "test" },
                    author = "Test",
                    version = "0.1",
                    execute = function(context, args) end,
                }
                """);

        Module module = parse(luaFile);
        assertNotNull(module);
        assertFalse(module.isScript());
        assertTrue(module.isSeeded());
        assertEquals(ModuleParser.hashNameToSeedOffset("seeded_module"), module.getSeedOffset());
    }

    @Test
    void unseededModulesSkipSeedOffsetMetadata() throws IOException {
        Path luaFile = writeScript("""
                return {
                    id = "unseeded_module",
                    name = "Unseeded Module",
                    groups = { "test" },
                    author = "Test",
                    version = "0.1",
                    seeded = false,
                    execute = function(context, args) end,
                }
                """);

        Module module = parse(luaFile);
        assertNotNull(module);
        assertFalse(module.isSeeded());
        assertEquals(0, module.getSeedOffset());
        assertFalse(module.isSeedOffsetFromMetadata());
    }

    private Path writeScript(String contents) throws IOException {
        Path luaFile = tempDir.resolve("module.lua");
        Files.writeString(luaFile, contents);
        return luaFile;
    }

    private Module parse(Path luaFile) {
        ErrorTracker.clearErrors();
        LuaSandbox sandbox = new LuaSandbox(java.util.List.of(tempDir.toString()));
        LuaValue loaded = new ModuleLoader(sandbox).loadFile(luaFile);
        if (loaded == null) {
            return null;
        }
        return ModuleParser.parse((LuaTable) loaded, luaFile);
    }
}
