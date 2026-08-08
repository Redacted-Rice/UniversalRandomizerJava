package redactedrice.randomizer.lua.dynamicVar;

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

import redactedrice.randomizer.lua.Issue;
import redactedrice.randomizer.lua.ExecutionPlan;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRepository;
import redactedrice.randomizer.utils.IssueTracker;

class DynamicVarValidatorTest {
    private ModuleRepository repository;
    private DynamicVarRegistry registry;

    @BeforeEach
    void setUp() {
        repository = new ModuleRepository(null, null);
        registry = DynamicVarRegistry.empty();
        IssueTracker.clear();
    }

    @Test
    void satisfiedNeedRecordsCompatibleProviders() {
        Module provider = module("set_evo_line_metadata", "Set Evo Line Metadata",
                List.of(new DynamicVar("evoLineId", "integer"),
                        new DynamicVar("evoLineMaxStage", "EvolutionStage")),
                List.of());
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));

        repository.registerModule(provider, m -> true);
        repository.registerModule(consumer, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);

        assertTrue(issues.isEmpty(), () -> issues.toString());
        assertEquals(List.of("set_evo_line_metadata"), registry.getNeedsForConsumer("fix_evo_line_hp")
                .get(0).getCompatibleProviderModuleIds());
    }

    @Test
    void needIsSatisfiedRegardlessOfModuleRegistrationOrder() {
        Module provider = module("set_evo_line_metadata", "Set Evo Line Metadata",
                List.of(new DynamicVar("evoLineId", "integer")), List.of());
        Module consumer = module("even_rando_evo_line_types", "Even Random Evo Line Types",
                List.of(), List.of(new DynamicVar("evoLineId", "integer")));

        repository.registerModule(consumer, m -> true);
        repository.registerModule(provider, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);

        assertTrue(issues.isEmpty(), () -> issues.toString());
    }

    @Test
    void duplicateProvideNameWithDifferentTypeIsWarning() {
        Module first = module("first_provider", "First Provider",
                List.of(new DynamicVar("evoLineId", "integer")), List.of());
        Module second = module("second_provider", "Second Provider",
                List.of(new DynamicVar("evoLineId", "string")), List.of());

        repository.registerModule(first, m -> true);
        repository.registerModule(second, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);

        assertEquals(1, issues.size());
        assertFalse(issues.get(0).isError());
        assertEquals("evoLineId", issues.get(0).getSubject());
    }

    @Test
    void missingProviderIsReported() {
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));
        repository.registerModule(consumer, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
        assertEquals("evoLineId", issues.get(0).getSubject());
    }

    @Test
    void moduleCannotSatisfyItsOwnNeedAtLoadTime() {
        Module selfSufficient = module("self_provider", "Self Provider",
                List.of(new DynamicVar("token", "integer")),
                List.of(new DynamicVar("token", "integer")));
        repository.registerModule(selfSufficient, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
        assertEquals("token", issues.get(0).getSubject());
        assertTrue(registry.getNeedsForConsumer("self_provider").get(0).getCompatibleProviders()
                .isEmpty());
    }

    @Test
    void typeMatchIsCaseInsensitiveAtLoadAndExecution() {
        Module provider = module("provider", "Provider",
                List.of(new DynamicVar("token", "Integer")), List.of());
        Module consumer = module("consumer", "Consumer", List.of(),
                List.of(new DynamicVar("token", "integer")));

        repository.registerModule(provider, m -> true);
        repository.registerModule(consumer, m -> true);

        List<Issue> loadIssues = DynamicVarValidator.validate(repository, registry, null);
        assertTrue(loadIssues.isEmpty(), () -> loadIssues.toString());

        List<Issue> orderIssues = DynamicVarValidator.validateExecutionPlan(
                ExecutionPlan.fromSteps(List.of(provider, consumer)), null);
        assertTrue(orderIssues.isEmpty(), () -> orderIssues.toString());
    }

    @Test
    void duplicateProvideSameNameDifferentCaseTypeIsNotConflict() {
        Module first = module("first_provider", "First Provider",
                List.of(new DynamicVar("token", "integer")), List.of());
        Module second = module("second_provider", "Second Provider",
                List.of(new DynamicVar("token", "Integer")), List.of());

        repository.registerModule(first, m -> true);
        repository.registerModule(second, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);
        assertTrue(issues.isEmpty(), () -> issues.toString());
    }

    @Test
    void typeMismatchDoesNotCountAsCompatibleProvider() {
        Module provider = module("wrong_type_provider", "Wrong Type Provider",
                List.of(new DynamicVar("evoLineId", "string")), List.of());
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));

        repository.registerModule(provider, m -> true);
        repository.registerModule(consumer, m -> true);

        List<Issue> issues = DynamicVarValidator.validate(repository, registry, null);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).getMessage().contains("evoLineId"));
    }

    @Test
    void executionPlanPassesWhenProviderRunsBeforeConsumer() {
        Module provider = module("set_evo_line_metadata", "Set Evo Line Metadata",
                List.of(new DynamicVar("evoLineId", "integer")), List.of());
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));

        List<Issue> issues = DynamicVarValidator.validateExecutionPlan(
                ExecutionPlan.fromSteps(List.of(provider, consumer)), null);

        assertTrue(issues.isEmpty(), () -> issues.toString());
    }

    @Test
    void executionPlanFailsWhenProviderRunsAfterConsumer() {
        Module provider = module("set_evo_line_metadata", "Set Evo Line Metadata",
                List.of(new DynamicVar("evoLineId", "integer")), List.of());
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));

        List<Issue> issues = DynamicVarValidator.validateExecutionPlan(
                ExecutionPlan.fromSteps(List.of(consumer, provider)), null);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
        assertTrue(issues.get(0).getMessage().contains("later"));
        assertEquals("evoLineId", issues.get(0).getSubject());
    }

    @Test
    void executionPlanFailsWhenProviderIsMissingFromPlan() {
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));

        List<Issue> issues = DynamicVarValidator.validateExecutionPlan(
                ExecutionPlan.fromSteps(List.of(consumer)), null);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
        assertTrue(issues.get(0).getMessage().contains("no earlier step"));
    }

    @Test
    void executionPlanFailsWhenEarlierProvideHasIncompatibleType() {
        Module wrongType = module("wrong_type", "Wrong Type",
                List.of(new DynamicVar("evoLineId", "string")), List.of());
        Module consumer = module("fix_evo_line_hp", "Fix Evo Line HP", List.of(),
                List.of(new DynamicVar("evoLineId", "integer")));

        List<Issue> issues = DynamicVarValidator.validateExecutionPlan(
                ExecutionPlan.fromSteps(List.of(wrongType, consumer)), null);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
        assertTrue(issues.get(0).getMessage().contains("incompatible"));
    }

    private static Module module(String id, String name, List<DynamicVar> provides,
            List<DynamicVar> needs) {
        LuaFunction execute = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
        return new Module(id, name, null, Set.of("test"), null, null, execute, null, null, 0, false,
                true, null, "author", "1.0.0", Map.of("ExampleApp", "1.0.0"), provides, needs, null,
                null, null);
    }
}
