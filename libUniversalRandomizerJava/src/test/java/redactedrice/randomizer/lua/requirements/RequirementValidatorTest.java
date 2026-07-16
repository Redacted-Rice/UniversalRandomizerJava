package redactedrice.randomizer.lua.requirements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRepository;
import redactedrice.randomizer.utils.ErrorTracker;

class RequirementValidatorTest {
    private ModuleRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ModuleRepository(null, null);
        ErrorTracker.clearErrors();
    }

    @Test
    void mandatoryPlatformMustBeDeclared() {
        Module module = module("consumer", "Consumer", Map.of(), "1.0.0");
        repository.registerModule(module, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
        assertEquals("ExampleApp", issues.get(0).getRequirementKey());
    }

    @Test
    void optionalPlatformIsCheckedOnlyWhenDeclared() {
        Module without = module("without", "Without", Map.of(), "1.0.0");
        Module withSatisfiedMinimum = module("with", "With",
                Map.of("ExampleApp", "1.0.0", "UniversalRandomizerJava", "0.4.0"), "1.0.0");
        repository.registerModule(without, m -> true);
        repository.registerModule(withSatisfiedMinimum, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);
        context.addCoreRequirement("UniversalRandomizerJava", "0.5.0", false);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertEquals(1, issues.size());
        assertTrue(issues.stream()
                .anyMatch(issue -> issue.isError() && issue.getModule().getId().equals("without")));
    }

    @Test
    void optionalPlatformFailsWhenBelowMinimum() {
        Module consumer = module("with", "With",
                Map.of("ExampleApp", "1.0.0", "UniversalRandomizerJava", "0.6.0"), "1.0.0");
        repository.registerModule(consumer, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);
        context.addCoreRequirement("UniversalRandomizerJava", "0.5.0", false);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertEquals(1, issues.size());
        assertFalse(issues.get(0).isError());
        assertEquals("UniversalRandomizerJava", issues.get(0).getRequirementKey());
    }

    @Test
    void moduleCanDependOnLoadedScript() {
        LuaFunction execute = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        Module setupScript = new Module("changedetector_setup", "Change Detector Setup", null, null,
                null, null, execute, null, null, 0, false, false, "randomize", "author", "1.0.0",
                Map.of("ExampleApp", "1.0.0"), null, null, null);
        Module consumer = new Module("downstream_action", "Downstream Action", null,
                Set.of("players"), null, null, execute, null, null, 0, false, true, null, "author",
                "1.0.0", Map.of("ExampleApp", "1.0.0", "changedetector_setup", "0.1.0"), null, null,
                null);

        repository.registerScript(setupScript, ModuleRepository.SCRIPT_TIMING_PRE);
        repository.registerModule(consumer, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertTrue(issues.isEmpty(), () -> issues.toString());
    }

    @Test
    void moduleDependencyPassesWhenLoadedVersionMeetsMinimum() {
        Module dependency =
                module("dependency", "Dependency", Map.of("ExampleApp", "1.0.0"), "0.2.0");
        Module consumer = module("consumer", "Consumer",
                Map.of("ExampleApp", "1.0.0", "dependency", "0.1.0"), "1.0.0");
        repository.registerModule(dependency, m -> true);
        repository.registerModule(consumer, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertTrue(issues.isEmpty(), () -> issues.toString());
    }

    @Test
    void moduleDependencyFailsWhenLoadedVersionIsBelowMinimum() {
        Module dependency =
                module("dependency", "Dependency", Map.of("ExampleApp", "1.0.0"), "0.1.0");
        Module consumer = module("consumer", "Consumer",
                Map.of("ExampleApp", "1.0.0", "dependency", "0.2.0"), "1.0.0");
        repository.registerModule(dependency, m -> true);
        repository.registerModule(consumer, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertEquals(1, issues.size());
        assertFalse(issues.get(0).isError());
        assertEquals("dependency", issues.get(0).getRequirementKey());
    }

    @Test
    void duplicateModuleIdIsRejected() {
        Module first = module("same_id", "First", Map.of("ExampleApp", "1.0.0"), "1.0.0");
        Module second = module("same_id", "Second", Map.of("ExampleApp", "1.0.0"), "1.0.0");

        assertTrue(repository.registerModule(first, m -> true));
        assertFalse(repository.registerModule(second, m -> true));
        assertEquals("First", repository.getModule("same_id").getName());
        assertTrue(ErrorTracker.hasErrors());
        assertTrue(ErrorTracker.getErrors().stream().anyMatch(msg -> msg.contains("same_id")));
    }

    @Test
    void duplicateScriptIdIsRejectedAndNotAddedTwice() {
        LuaFunction execute = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        Module first = new Module("shared_script", "First Script", null, null, null, null, execute,
                null, null, 0, false, false, "randomize", "author", "1.0.0",
                Map.of("ExampleApp", "1.0.0"), null, null, null);
        Module second = new Module("shared_script", "Second Script", null, null, null, null, execute,
                null, null, 0, false, false, "randomize", "author", "1.0.0",
                Map.of("ExampleApp", "1.0.0"), null, null, null);

        assertTrue(repository.registerScript(first, ModuleRepository.SCRIPT_TIMING_PRE));
        assertFalse(repository.registerScript(second, ModuleRepository.SCRIPT_TIMING_PRE));
        assertEquals(1, repository.getScripts(ModuleRepository.SCRIPT_TIMING_PRE,
                ModuleRepository.SCRIPT_WHEN_RANDOMIZE).size());
        assertEquals("First Script", repository.getScript("shared_script").getName());
    }

    @Test
    void missingModuleDependencyIsReported() {
        Module consumer = module("consumer", "Consumer",
                Map.of("ExampleApp", "1.0.0", "missing_dep", "0.1.0"), "1.0.0");
        repository.registerModule(consumer, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
    }

    @Test
    void moduleDependencyIsResolvedRegardlessOfRegistrationOrder() {
        Module consumer = module("consumer", "Consumer",
                Map.of("ExampleApp", "1.0.0", "dependency", "0.1.0"), "1.0.0");
        Module dependency =
                module("dependency", "Dependency", Map.of("ExampleApp", "1.0.0"), "0.2.0");

        repository.registerModule(consumer, m -> true);
        repository.registerModule(dependency, m -> true);

        CoreRequirements context = requirements("ExampleApp", "1.0.0", true);

        List<RequirementIssue> issues = RequirementValidator.validate(context, repository);
        assertTrue(issues.isEmpty(), () -> issues.toString());
    }

    private static CoreRequirements requirements(String key, String version, boolean mandatory) {
        CoreRequirements context = new CoreRequirements();
        context.addCoreRequirement(key, version, mandatory);
        return context;
    }

    private Module module(String id, String name, Map<String, String> requires, String version) {
        LuaFunction execute = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        return new Module(id, name, null, Set.of("test"), null, null, execute, null, null, 0, false,
                true, null, "author", version, requires, null, null, null);
    }
}
