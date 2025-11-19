package redactedrice.randomizer;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.wrapper.RandomizerResourceExtractor;
import redactedrice.support.test.TestEntity;
import redactedrice.randomizer.wrapper.LuaRandomizerWrapper;
import redactedrice.randomizer.wrapper.ExecutionResult;
import redactedrice.randomizer.wrapper.ExecutionRequest;
import redactedrice.randomizer.metadata.LuaModuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for LuaRandomizerWrapper - similar to appExample. Tests end-to-end functionality
 * of loading and executing modules.
 */
public class RandomizerWrapperTest {

    private LuaRandomizerWrapper wrapper;
    private String randomizerPath;
    private String modulesPath;

    @BeforeEach
    public void setUp() {
        randomizerPath = new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        RandomizerResourceExtractor.setPath(randomizerPath);
        modulesPath = new File("src/test/java/redactedrice/support/lua_modules").getAbsolutePath();
        wrapper = new LuaRandomizerWrapper(modulesPath);
    }

    @Test
    public void testLoadAndExecuteModules() {
        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0, "Should load at least one module");

        Set<String> moduleNames = wrapper.getModuleNames();
        assertTrue(moduleNames.contains("Simple Entity Randomizer"));
    }

    @Test
    public void testOptionalMetadataFieldsParsed() {
        wrapper.loadModules();

        LuaModuleMetadata module = wrapper.getModule("Simple Entity Randomizer");
        assertNotNull(module, "Simple Entity Randomizer module should be loaded");

        // Verify optional fields are parsed correctly
        assertEquals("https://github.com/not/a/real/url", module.getSource(),
                "Source field should be parsed correctly");
        assertEquals("MIT", module.getLicense(), "License field should be parsed correctly");
        assertEquals("Just a module designed for use in testing the randomizer wrapper.",
                module.getAbout(), "About field should be parsed correctly");
    }

    @Test
    public void testOptionalMetadataFieldsCanBeNull() {
        wrapper.loadModules();

        // Find a module that doesn't have optional fields (e.g., Advanced Entity Randomizer)
        LuaModuleMetadata module = wrapper.getModule("Advanced Entity Randomizer");
        if (module != null) {
            // Optional fields should be null if not specified in the module
            assertNull(module.getSource(),
                    "Source field should be null when not specified in module");
            assertNull(module.getLicense(),
                    "License field should be null when not specified in module");
            assertNull(module.getAbout(),
                    "About field should be null when not specified in module");
        }
    }

    @Test
    public void testExecuteModuleWithArguments() {
        wrapper.loadModules();

        TestEntity entity = new TestEntity("Original", 100, 10.0, true);

        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("healthMin", 50);
        arguments.put("healthMax", 200);
        arguments.put("damageMultiplier", 1.5);

        ExecutionRequest request = ExecutionRequest.withDefaultSeedOffset("Simple Entity Randomizer", arguments, 0, wrapper.getModuleRegistry());
        ExecutionResult result = wrapper.executeModule(request, context);

        assertTrue(result.isSuccess());
        assertNotEquals("Original", entity.getName());
        assertTrue(entity.getHealth() >= 50 && entity.getHealth() <= 200);
        assertEquals(15.0, entity.getDamage(), 0.01);
    }

    @Test
    public void testExecuteModuleWithComplexArguments() {
        wrapper.loadModules();

        TestEntity entity = new TestEntity("Original", 100, 10.0, true);

        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("entityType", "warrior");
        arguments.put("level", 11);
        arguments.put("applyBonus", true);

        ExecutionRequest request = ExecutionRequest.withDefaultSeedOffset("Advanced Entity Randomizer", arguments, 0, wrapper.getModuleRegistry());
        ExecutionResult result = wrapper.executeModule(request, context);

        assertTrue(result.isSuccess());
        assertNotEquals("Original", entity.getName());
        assertTrue(entity.getHealth() > 100);
        assertTrue(entity.getDamage() > 10.0);
    }

    @Test
    public void testSeedReproducibility() {
        wrapper.loadModules();

        Map<String, Object> args = new HashMap<>();
        args.put("healthMin", 50);
        args.put("healthMax", 200);
        args.put("damageMultiplier", 1.5);

        TestEntity entity1 = new TestEntity("Hero", 100, 10.0, true);
        JavaContext context1 = new JavaContext();
        context1.register("entity", entity1);
        ExecutionRequest request1 = ExecutionRequest.withSeed("Simple Entity Randomizer", args, 999);
        wrapper.executeModule(request1, context1);

        TestEntity entity2 = new TestEntity("Hero", 100, 10.0, true);
        JavaContext context2 = new JavaContext();
        context2.register("entity", entity2);
        ExecutionRequest request2 = ExecutionRequest.withSeed("Simple Entity Randomizer", args, 999);
        wrapper.executeModule(request2, context2);

        assertEquals(entity1.getName(), entity2.getName());
        assertEquals(entity1.getHealth(), entity2.getHealth());
        assertEquals(entity1.getDamage(), entity2.getDamage(), 0.01);
    }

    @Test
    public void testExecuteMultipleModules() {
        wrapper.loadModules();

        TestEntity entity1 = new TestEntity("Entity1", 100, 10.0, true);
        TestEntity entity2 = new TestEntity("Entity2", 150, 15.0, true);

        JavaContext context1 = new JavaContext();
        context1.register("entity", entity1);
        Map<String, Object> args1 = new HashMap<>();
        args1.put("healthMin", 80);
        args1.put("healthMax", 120);
        args1.put("damageMultiplier", 1.2);
        ExecutionRequest request1 = ExecutionRequest.withDefaultSeedOffset("Simple Entity Randomizer", args1, 0, wrapper.getModuleRegistry());
        wrapper.executeModule(request1, context1);

        JavaContext context2 = new JavaContext();
        context2.register("entity", entity2);
        Map<String, Object> args2 = new HashMap<>();
        args2.put("entityType", "mage");
        args2.put("level", 16);
        args2.put("applyBonus", false);
        ExecutionRequest request2 = ExecutionRequest.withDefaultSeedOffset("Advanced Entity Randomizer", args2, 0, wrapper.getModuleRegistry());
        wrapper.executeModule(request2, context2);

        assertNotEquals("Entity1", entity1.getName());
        assertNotEquals("Entity2", entity2.getName());
        assertFalse(wrapper.hasErrors());
    }

    @Test
    public void testArgumentValidationFails() {
        wrapper.loadModules();

        TestEntity entity = new TestEntity();
        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> badArgs = new HashMap<>();
        badArgs.put("entityType", "invalid_type");
        badArgs.put("level", 1);
        badArgs.put("applyBonus", true);

        ExecutionRequest request = ExecutionRequest.withDefaultSeedOffset("Advanced Entity Randomizer", badArgs, 0, wrapper.getModuleRegistry());
        ExecutionResult result = wrapper.executeModule(request, context);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    public void testContextWithMultipleObjects() {
        wrapper.loadModules();

        TestEntity entity = new TestEntity("Hero", 100, 10.0, true);
        List<String> namePool = Arrays.asList("Custom1", "Custom2", "Custom3");

        JavaContext context = new JavaContext();
        context.register("entity", entity);
        context.register("customNames", namePool);

        Map<String, Object> args = new HashMap<>();
        args.put("healthMin", 90);
        args.put("healthMax", 110);
        args.put("damageMultiplier", 1.0);

        ExecutionRequest request = ExecutionRequest.withDefaultSeedOffset("Simple Entity Randomizer", args, 0, wrapper.getModuleRegistry());
        wrapper.executeModule(request, context);

        assertTrue(context.contains("entity"));
        assertTrue(context.contains("customNames"));
    }

    @Test
    public void testErrorHandling() {
        wrapper.loadModules();

        JavaContext emptyContext = new JavaContext();
        Map<String, Object> args = new HashMap<>();
        args.put("healthMin", 50);
        args.put("healthMax", 100);
        args.put("damageMultiplier", 1.0);

        ExecutionRequest request = ExecutionRequest.withDefaultSeedOffset("Simple Entity Randomizer", args, 0, wrapper.getModuleRegistry());
        ExecutionResult result = wrapper.executeModule(request, emptyContext);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // Helper class to hold common test data
    private static class BatchTestData {
        TestEntity entity1;
        TestEntity entity2;
        JavaContext context;
        Map<String, Map<String, Object>> argumentsPerModule;
        Map<String, Integer> seedsPerModule;
        List<String> moduleNames;
        List<ExecutionRequest> requests;

        BatchTestData() {
            int seed = 12345;
            this.entity1 = new TestEntity("Entity1", 100, 10.0, true);
            this.entity2 = new TestEntity("Entity2", 150, 15.0, true);
            this.context = new JavaContext();
            this.context.register("entity", entity1);

            this.moduleNames =
                    Arrays.asList("Simple Entity Randomizer", "Advanced Entity Randomizer");

            // Setup arguments for multiple modules
            this.argumentsPerModule = new HashMap<>();

            Map<String, Object> args1 = new HashMap<>();
            args1.put("healthMin", 80);
            args1.put("healthMax", 120);
            args1.put("damageMultiplier", 1.2);
            argumentsPerModule.put(moduleNames.get(0), args1);

            Map<String, Object> args2 = new HashMap<>();
            args2.put("entityType", "warrior");
            args2.put("level", 11);
            args2.put("applyBonus", true);
            argumentsPerModule.put(moduleNames.get(1), args2);

            // Setup seeds for modules
            this.seedsPerModule = new HashMap<>();
            seedsPerModule.put(moduleNames.get(0), seed);
            seedsPerModule.put(moduleNames.get(1), seed + 1);

            // Initialize requests list
            this.requests = new ArrayList<>();
            requests.add(ExecutionRequest.withSeed(moduleNames.get(0),
                    argumentsPerModule.get(moduleNames.get(0)),
                    seedsPerModule.get(moduleNames.get(0))));
            requests.add(ExecutionRequest.withSeed(moduleNames.get(1),
                    argumentsPerModule.get(moduleNames.get(1)),
                    seedsPerModule.get(moduleNames.get(1))));
        }
    }

    // Common verification for BatchTestData tests
    private void verifyBatchTestDataExecution() {
        List<ExecutionResult> results = wrapper.getExecutionResults();

        // Should have 8 total results: 2 modules + 6 scripts
        assertEquals(8, results.size(), "Should have 2 modules and 6 script executions");

        // Count module and script executions
        int simpleModuleCount = 0;
        int advancedModuleCount = 0;
        int preRandomizeScriptCount = 0;
        int preModuleScriptCount = 0;
        int postModuleScriptCount = 0;
        int postRandomizeScriptCount = 0;

        for (ExecutionResult result : results) {
            String moduleName = result.getModuleName();
            if (moduleName.equals("Simple Entity Randomizer")) {
                simpleModuleCount++;
                assertTrue(result.isSuccess());
            } else if (moduleName.equals("Advanced Entity Randomizer")) {
                advancedModuleCount++;
                assertTrue(result.isSuccess());
            } else if (moduleName.equals("Test Pre Randomize Script")) {
                preRandomizeScriptCount++;
                assertTrue(result.isSuccess());
            } else if (moduleName.equals("Test Pre Module Script")) {
                preModuleScriptCount++;
                assertTrue(result.isSuccess());
            } else if (moduleName.equals("Test Post Module Script")) {
                postModuleScriptCount++;
                assertTrue(result.isSuccess());
            } else if (moduleName.equals("Test Post Randomize Script")) {
                postRandomizeScriptCount++;
                assertTrue(result.isSuccess());
            }
        }

        // Verify module counts
        assertEquals(1, simpleModuleCount);
        assertEquals(1, advancedModuleCount);

        // Verify script counts
        assertEquals(1, preRandomizeScriptCount);
        assertEquals(2, preModuleScriptCount);
        assertEquals(2, postModuleScriptCount);
        assertEquals(1, postRandomizeScriptCount);

        assertFalse(wrapper.hasErrors());
    }

    @Test
    public void testBatchProcessingWithPrePostRandomizeScripts() {
        wrapper.loadModules();
        BatchTestData data = new BatchTestData();

        // Execute in batch. This automatically runs all the scripts
        List<ExecutionResult> results = wrapper.executeModules(data.requests, data.context);
        assertEquals(2, results.size());

        verifyBatchTestDataExecution();
    }

    @Test
    public void testIndividualProcessingWithPrePostRandomizeScripts() {
        wrapper.loadModules();
        BatchTestData data = new BatchTestData();

        // Manually execute pre randomize scripts
        wrapper.executePreRandomizeScripts(data.context);

        // Execute modules individually. This will run the pre/post module scripts but not the
        // pre/post randomize scripts
        wrapper.executeModule(data.requests.get(0), data.context);
        wrapper.executeModule(data.requests.get(1), data.context);

        // Manually execute post randomize scripts
        wrapper.executePostRandomizeScripts(data.context);

        verifyBatchTestDataExecution();
    }
}

