package redactedrice.randomizer.lua.dynamicVar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redactedrice.randomizer.lua.ExecutionPlan;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRegistry;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.utils.IssueTracker;

class ExecutionPlanTest {
    @TempDir
    Path tempDir;

    @Test
    void batchPlanTreatsPreModuleScriptAsProvider() throws IOException {
        ModuleRegistry registry = loadRegistry();
        Module consumer = registry.getModule("dv_consumer");

        IssueTracker.clear();
        ExecutionPlan plan = ExecutionPlan.forRandomizeBatch(registry,
                List.of(ExecutionRequest.forModule(consumer, null)));

        assertTrue(plan.validate(), () -> IssueTracker.getErrors().toString());
    }

    private ModuleRegistry loadRegistry() throws IOException {
        Path root = tempDir.resolve("mods");
        Path actions = root.resolve("actions");
        Path prescripts = root.resolve("prescripts");
        Files.createDirectories(actions);
        Files.createDirectories(prescripts);

        Files.writeString(prescripts.resolve("dv_setup.lua"), """
                return {
                    id = "dv_setup",
                    name = "Setup",
                    when = "module",
                    author = "test",
                    version = "1.0",
                    provides = { { name = "token", type = "integer" } },
                    execute = function() end,
                }
                """);

        Files.writeString(actions.resolve("dv_consumer.lua"), """
                return {
                    id = "dv_consumer",
                    name = "Consumer",
                    groups = { "test" },
                    author = "test",
                    version = "1.0",
                    needs = { { name = "token", type = "integer" } },
                    execute = function() end,
                }
                """);

        String randomizerPath =
                new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        LuaSandbox sandbox = new LuaSandbox(List.of(randomizerPath, root.toString()));
        ModuleRegistry registry = new ModuleRegistry(sandbox);
        registry.loadModulesFromDirectory(root.toString());
        return registry;
    }
}
