package redactedrice.randomizer.lua.dynamicVar;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.utils.IssueTracker;

class DynamicVarExecutionIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void executeModulesReturnsNullWhenProviderRunsAfterConsumer() throws IOException {
        Path root = writeModules();
        String randomizerPath =
                new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        LuaRandomizerWrapper wrapper = new LuaRandomizerWrapper(
                List.of(randomizerPath, root.toString()), List.of(root.toString()));
        wrapper.loadModules();

        Module consumer = wrapper.getModule("dv_consumer");
        Module provider = wrapper.getModule("dv_provider");
        JavaContext context = new JavaContext();

        IssueTracker.clear();
        List<ExecutionResult> results = wrapper.executeModules(
                List.of(ExecutionRequest.forModule(consumer, null),
                        ExecutionRequest.forModule(provider, null)),
                context, 12345);

        assertNull(results);
        assertTrue(IssueTracker.hasErrors());
        assertTrue(IssueTracker.getErrors().stream().anyMatch(e -> e.contains("later")));
    }

    private Path writeModules() throws IOException {
        Path root = tempDir.resolve("mods");
        Path actions = root.resolve("actions");
        Files.createDirectories(actions);

        Files.writeString(actions.resolve("dv_provider.lua"), """
                return {
                    id = "dv_provider",
                    name = "Provider",
                    groups = { "test" },
                    author = "test",
                    version = "1.0",
                    provides = { { name = "token", type = "integer" } },
                    execute = function() return true end,
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
                    execute = function() return true end,
                }
                """);
        return root;
    }
}
