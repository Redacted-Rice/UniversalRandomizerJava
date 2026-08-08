package redactedrice.randomizer.lua.dynamicVar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleLoader;
import redactedrice.randomizer.lua.ModuleParser;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.utils.IssueTracker;

class DynamicVarParserTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesProvidesAndNeedsWithTypes() throws IOException {
        Module module = parse(writeModule("""
                provides = {
                    { name = "evoLineId", type = "integer" },
                    { name = "evoLineMaxStage", type = "EvolutionStage" },
                },
                needs = {
                    { name = "numMoves", type = "integer" },
                },
                """));
        assertTrue(IssueTracker.getErrors().isEmpty(), () -> IssueTracker.getErrors().toString());

        assertEquals(2, module.getProvides().size());
        assertEquals("evoLineId", module.getProvides().get(0).getName());
        assertEquals("integer", module.getProvides().get(0).getType());
        assertEquals("EvolutionStage", module.getProvides().get(1).getType());

        assertEquals(1, module.getNeeds().size());
        assertEquals("numMoves", module.getNeeds().get(0).getName());
        assertEquals("integer", module.getNeeds().get(0).getType());
    }

    @Test
    void rejectsProvidesWhenNotATable() throws IOException {
        Module module = parse(writeModule("""
                provides = "not a table",
                """));
        assertNull(module);
        assertTrue(IssueTracker.getErrors().stream().anyMatch(e -> e.contains("provides")));
    }

    @Test
    void rejectsDuplicateProvideNamesOnSameModule() throws IOException {
        Module module = parse(writeModule("""
                provides = {
                    { name = "token", type = "integer" },
                    { name = "token", type = "integer" },
                },
                """));
        assertNull(module);
        assertTrue(IssueTracker.getErrors().stream().anyMatch(e -> e.contains("duplicate")));
    }

    @Test
    void rejectsProvideEntryMissingType() throws IOException {
        Module module = parse(writeModule("""
                provides = {
                    { name = "token" },
                },
                """));
        assertNull(module);
        assertTrue(IssueTracker.getErrors().stream().anyMatch(e -> e.contains("type")));
    }

    @Test
    void rejectsMapStyleProvides() throws IOException {
        Module module = parse(writeModule("""
                provides = {
                    evoLineId = { name = "evoLineId", type = "integer" },
                },
                """));
        assertNull(module);
        assertTrue(IssueTracker.getErrors().stream().anyMatch(e -> e.contains("array")));
    }

    @Test
    void preservesArrayDeclarationOrder() throws IOException {
        Module module = parse(writeModule("""
                provides = {
                    { name = "first", type = "integer" },
                    { name = "second", type = "string" },
                    { name = "third", type = "boolean" },
                },
                """));
        assertTrue(IssueTracker.getErrors().isEmpty(), () -> IssueTracker.getErrors().toString());
        assertEquals(List.of("first", "second", "third"),
                module.getProvides().stream().map(DynamicVar::getName).toList());
    }

    private Path writeModule(String extraFields) throws IOException {
        Path file = Files.createTempFile(tempDir, "module", ".lua");
        Files.writeString(file, """
                return {
                    id = "test_module",
                    name = "Test Module",
                    groups = { "test" },
                    author = "author",
                    version = "1.0.0",
                %s
                    execute = function() end,
                }
                """.formatted(extraFields.strip()));
        return file;
    }

    private Module parse(Path file) {
        IssueTracker.clear();
        LuaSandbox sandbox = new LuaSandbox(List.of(tempDir.toString()));
        LuaValue loaded = new ModuleLoader(sandbox).loadFile(file);
        return ModuleParser.parse((LuaTable) loaded, file);
    }
}
