package redactedrice.randomizer.lua.dynamicVar;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Path file = tempDir.resolve("provider.lua");
        Files.writeString(file, """
                return {
                    id = "provider",
                    name = "Provider",
                    groups = { "test" },
                    author = "author",
                    version = "1.0.0",
                    provides = {
                        { name = "evoLineId", type = "integer" },
                        { name = "evoLineMaxStage", type = "EvolutionStage" },
                    },
                    needs = {
                        { name = "numMoves", type = "integer" },
                    },
                    execute = function() end,
                }
                """);

        Module module = parse(file);
        assertTrue(IssueTracker.getErrors().isEmpty(), () -> IssueTracker.getErrors().toString());

        assertEquals(2, module.getProvides().size());
        assertEquals("evoLineId", module.getProvides().get(0).getName());
        assertEquals("integer", module.getProvides().get(0).getType());
        assertEquals("EvolutionStage", module.getProvides().get(1).getType());

        assertEquals(1, module.getNeeds().size());
        assertEquals("numMoves", module.getNeeds().get(0).getName());
        assertEquals("integer", module.getNeeds().get(0).getType());
    }

    private Module parse(Path file) {
        IssueTracker.clear();
        LuaSandbox sandbox = new LuaSandbox(List.of(tempDir.toString()));
        LuaValue loaded = new ModuleLoader(sandbox).loadFile(file);
        return ModuleParser.parse((LuaTable) loaded, file);
    }
}
