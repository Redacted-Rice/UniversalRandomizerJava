package redactedrice.randomizer.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.utils.IssueTracker;

public class ModuleParserTest {

    @TempDir
    Path tempDir;

    @Test
    public void hashIdToSeedOffsetIsStableAndInRange() {
        int offset = ModuleParser.hashIdToSeedOffset("generated_module");
        assertTrue(offset >= 1 && offset <= 9999);
        assertEquals(ModuleParser.hashIdToSeedOffset("module_a"),
                ModuleParser.hashIdToSeedOffset("module_a"));
        assertNotEquals(ModuleParser.hashIdToSeedOffset("module_a"),
                ModuleParser.hashIdToSeedOffset("module_b"));
    }

    @Test
    void scriptsDefaultToUnseededWithoutSeedMetadata() throws IOException {
        Module module = parse(writeScript("""
                return {
                    id = "seedless_script",
                    name = "Seedless Script",
                    description = "test",
                    when = "randomize",
                    author = "Test",
                    version = "0.1",
                    execute = function(context) end,
                }
                """));

        assertNotNull(module);
        assertTrue(module.isScript());
        assertFalse(module.isSeeded());
        assertEquals(0, module.getSeedOffset());
        assertFalse(module.isSeedOffsetFromMetadata());
    }

    @Test
    void scriptsRejectSeedMetadataButStillLoad() throws IOException {
        Module defaultOffset = parse(writeScript("""
                return {
                    id = "bad_script",
                    name = "Bad Script",
                    when = "randomize",
                    author = "Test",
                    version = "0.1",
                    defaultSeedOffset = 10,
                    execute = function(context) end,
                }
                """));
        assertNotNull(defaultOffset);
        assertTrue(defaultOffset.isScript());
        assertFalse(defaultOffset.isSeeded());
        assertEquals(0, defaultOffset.getSeedOffset());
        assertTrue(IssueTracker.hasErrors());

        IssueTracker.clear();
        Module seededField = parse(writeScript("""
                return {
                    id = "bad_script_seeded",
                    name = "Bad Script",
                    when = "randomize",
                    author = "Test",
                    version = "0.1",
                    seeded = true,
                    execute = function(context) end,
                }
                """));
        assertNotNull(seededField);
        assertTrue(seededField.isScript());
        assertFalse(seededField.isSeeded());
        assertEquals(0, seededField.getSeedOffset());
        assertTrue(IssueTracker.hasErrors());
    }

    @Test
    void modulesUseSeededDefaultsOrExplicitUnseededFlag() throws IOException {
        Module seeded = parse(writeScript("""
                return {
                    id = "seeded_module",
                    name = "Seeded Module",
                    groups = { "test" },
                    author = "Test",
                    version = "0.1",
                    execute = function(context, args) end,
                }
                """));
        assertNotNull(seeded);
        assertFalse(seeded.isScript());
        assertTrue(seeded.isSeeded());
        assertEquals(ModuleParser.hashIdToSeedOffset("seeded_module"), seeded.getSeedOffset());

        Module unseeded = parse(writeScript("""
                return {
                    id = "unseeded_module",
                    name = "Unseeded Module",
                    groups = { "test" },
                    author = "Test",
                    version = "0.1",
                    seeded = false,
                    execute = function(context, args) end,
                }
                """));
        assertNotNull(unseeded);
        assertFalse(unseeded.isSeeded());
        assertEquals(0, unseeded.getSeedOffset());
        assertFalse(unseeded.isSeedOffsetFromMetadata());
    }

    private Path writeScript(String contents) throws IOException {
        Path luaFile = tempDir.resolve("module.lua");
        Files.writeString(luaFile, contents);
        return luaFile;
    }

    private Module parse(Path luaFile) {
        IssueTracker.clear();
        LuaSandbox sandbox = new LuaSandbox(java.util.List.of(tempDir.toString()));
        LuaValue loaded = new ModuleLoader(sandbox).loadFile(luaFile);
        if (loaded == null) {
            return null;
        }
        return ModuleParser.parse((LuaTable) loaded, luaFile);
    }
}
