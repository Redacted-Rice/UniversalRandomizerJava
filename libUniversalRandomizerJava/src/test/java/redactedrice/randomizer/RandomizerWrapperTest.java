package redactedrice.randomizer;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.ErrorTracker;
import redactedrice.support.test.TestEntity;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.Module;
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

    private static final int TEST_BASE_SEED = 12345;

    private LuaRandomizerWrapper wrapper;
    private String randomizerPath;
    private String modulesPath;

    @BeforeEach
    public void setUp() {
        randomizerPath = new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        modulesPath = new File("src/test/java/redactedrice/support/lua_modules").getAbsolutePath();

        // Define allowed directories: randomizer + modules
        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(randomizerPath);
        allowedDirectories.add(modulesPath);

        // Search paths for module discovery
        List<String> searchPaths = new ArrayList<>();
        searchPaths.add(modulesPath);

        wrapper = new LuaRandomizerWrapper(allowedDirectories, searchPaths);
    }

    @Test
    public void testLoadAndExecuteModules() {
        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0, "Should load at least one module");

        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("simple_entity_randomizer"));
    }

    @Test
    public void testOptionalMetadataFieldsParsed() {
        wrapper.loadModules();

        Module module = wrapper.getModule("simple_entity_randomizer");
        assertNotNull(module, "simple_entity_randomizer module should be loaded");

        // Verify optional fields are parsed correctly
        assertEquals("https://github.com/not/a/real/url", module.getSource(),
                "Source field should be parsed correctly");
        assertEquals("MIT", module.getLicense(), "License field should be parsed correctly");
        assertEquals("Just a module designed for use in testing the randomizer wrapper.",
                module.getAbout(), "About field should be parsed correctly");
        assertTrue(module.isSeedOffsetFromMetadata());
        assertEquals(42, module.getSeedOffset());
    }

    @Test
    public void testOptionalMetadataFieldsCanBeNull() {
        wrapper.loadModules();

        // Find a module that doesn't have optional fields (e.g., advanced_entity_randomizer)
        Module module = wrapper.getModule("advanced_entity_randomizer");
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

        ExecutionRequest request =
                ExecutionRequest.forModule(wrapper.getModule("simple_entity_randomizer"), arguments);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);

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

        ExecutionRequest request = ExecutionRequest
                .forModule(wrapper.getModule("advanced_entity_randomizer"), arguments);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);

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
        ExecutionRequest request1 = ExecutionRequest
                .forModuleWithSeedOffset("simple_entity_randomizer", args, 999);
        wrapper.executeModule(request1, context1, TEST_BASE_SEED);

        TestEntity entity2 = new TestEntity("Hero", 100, 10.0, true);
        JavaContext context2 = new JavaContext();
        context2.register("entity", entity2);
        ExecutionRequest request2 = ExecutionRequest
                .forModuleWithSeedOffset("simple_entity_randomizer", args, 999);
        wrapper.executeModule(request2, context2, TEST_BASE_SEED);

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
        ExecutionRequest request1 = ExecutionRequest
                .forModule(wrapper.getModule("simple_entity_randomizer"), args1);
        wrapper.executeModule(request1, context1, TEST_BASE_SEED);

        JavaContext context2 = new JavaContext();
        context2.register("entity", entity2);
        Map<String, Object> args2 = new HashMap<>();
        args2.put("entityType", "mage");
        args2.put("level", 16);
        args2.put("applyBonus", false);
        ExecutionRequest request2 = ExecutionRequest
                .forModule(wrapper.getModule("advanced_entity_randomizer"), args2);
        wrapper.executeModule(request2, context2, TEST_BASE_SEED);

        assertNotEquals("Entity1", entity1.getName());
        assertNotEquals("Entity2", entity2.getName());
        assertFalse(ErrorTracker.hasErrors());
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

        ExecutionRequest request = ExecutionRequest
                .forModule(wrapper.getModule("advanced_entity_randomizer"), badArgs);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);
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

        ExecutionRequest request = ExecutionRequest
                .forModule(wrapper.getModule("simple_entity_randomizer"), args);
        wrapper.executeModule(request, context, TEST_BASE_SEED);

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

        ExecutionRequest request = ExecutionRequest
                .forModule(wrapper.getModule("simple_entity_randomizer"), args);
        ExecutionResult result = wrapper.executeModule(request, emptyContext, TEST_BASE_SEED);

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
            this.entity1 = new TestEntity("Entity1", 100, 10.0, true);
            this.entity2 = new TestEntity("Entity2", 150, 15.0, true);
            this.context = new JavaContext();
            this.context.register("entity", entity1);

            this.moduleNames =
                    Arrays.asList("simple_entity_randomizer", "advanced_entity_randomizer");

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
            seedsPerModule.put(moduleNames.get(0), 0);
            seedsPerModule.put(moduleNames.get(1), 1);

            // Initialize requests list
            this.requests = new ArrayList<>();
            requests.add(ExecutionRequest.forModuleWithSeedOffset(moduleNames.get(0),
                    argumentsPerModule.get(moduleNames.get(0)),
                    seedsPerModule.get(moduleNames.get(0))));
            requests.add(ExecutionRequest.forModuleWithSeedOffset(moduleNames.get(1),
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
            String moduleId = result.getModuleId();
            if (moduleId.equals("simple_entity_randomizer")) {
                simpleModuleCount++;
                assertTrue(result.isSuccess());
            } else if (moduleId.equals("advanced_entity_randomizer")) {
                advancedModuleCount++;
                assertTrue(result.isSuccess());
            } else if (moduleId.equals("test_pre_randomize")) {
                preRandomizeScriptCount++;
                assertTrue(result.isSuccess());
                assertTrue(result.getRequest().isScript());
            } else if (moduleId.equals("test_pre_module")) {
                preModuleScriptCount++;
                assertTrue(result.isSuccess());
                assertTrue(result.getRequest().isScript());
            } else if (moduleId.equals("test_post_module")) {
                postModuleScriptCount++;
                assertTrue(result.isSuccess());
                assertTrue(result.getRequest().isScript());
            } else if (moduleId.equals("test_post_randomize")) {
                postRandomizeScriptCount++;
                assertTrue(result.isSuccess());
                assertTrue(result.getRequest().isScript());
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

        assertFalse(findLoadedModule("test_pre_randomize").isSeeded());
        assertFalse(findLoadedModule("test_pre_module").isSeeded());
        assertNotNull(wrapper.getScript("test_pre_randomize"));
        assertNotNull(wrapper.getScript("test_pre_module"));
        assertNull(wrapper.getModule("test_pre_randomize"));
        assertNull(wrapper.getModule("test_pre_module"));
        assertTrue(wrapper.getModule("simple_entity_randomizer").isSeeded());

        assertFalse(ErrorTracker.hasErrors());
    }

    private Module findLoadedModule(String name) {
        Module module = wrapper.getModule(name);
        if (module != null) {
            return module;
        }
        module = wrapper.getScript(name);
        if (module != null) {
            return module;
        }
        fail("Module not loaded: " + name);
        return null;
    }

    @Test
    public void testBatchProcessingWithPrePostRandomizeScripts() {
        wrapper.loadModules();
        BatchTestData data = new BatchTestData();

        // Execute in batch. This automatically runs all the scripts
        List<ExecutionResult> results =
                wrapper.executeModules(data.requests, data.context, TEST_BASE_SEED);
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
        wrapper.executeModule(data.requests.get(0), data.context, TEST_BASE_SEED);
        wrapper.executeModule(data.requests.get(1), data.context, TEST_BASE_SEED);

        // Manually execute post randomize scripts
        wrapper.executePostRandomizeScripts(data.context);

        verifyBatchTestDataExecution();
    }

    @Test
    public void testExplicitSeedOverridesModuleDefault() {
        wrapper.loadModules();
        TestEntity entity = new TestEntity("Hero", 100, 10.0, true);
        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> args = new HashMap<>();
        args.put("healthMin", 50);
        args.put("healthMax", 200);
        args.put("damageMultiplier", 1.5);

        Module module = wrapper.getModule("simple_entity_randomizer");
        int defaultSeed = TEST_BASE_SEED + module.getSeedOffset();

        ExecutionRequest explicitRequest = ExecutionRequest
                .forModuleWithSeedOffset("simple_entity_randomizer", args, 424242);
        ExecutionResult result = wrapper.executeModule(explicitRequest, context, TEST_BASE_SEED);

        assertTrue(result.isSuccess());
        assertEquals(TEST_BASE_SEED + 424242, result.getSeedUsed());
        assertNotEquals(defaultSeed, result.getSeedUsed());
    }

    @Test
    public void testGetModulesByGroup() {
        wrapper.loadModules();

        // Get all groups
        Set<String> groupKeys = wrapper.getDefinedGroupValues();
        assertNotNull(groupKeys);
        assertTrue(groupKeys.contains("basic"));
        assertTrue(groupKeys.contains("advanced"));
        assertTrue(groupKeys.contains("gameplay"));
        assertTrue(groupKeys.contains("utils"));

        // Get modules in the basic group
        List<Module> basicModules = wrapper.getModulesByGroup("basic");
        assertNotNull(basicModules);
        assertEquals(1, basicModules.size());
        assertEquals("Simple Entity Randomizer", basicModules.get(0).getName());

        // Get modules in the advanced group
        List<Module> advancedModules = wrapper.getModulesByGroup("advanced");
        assertNotNull(advancedModules);
        assertEquals(3, advancedModules.size());
        Set<String> advancedNames = new HashSet<>();
        for (Module module : advancedModules) {
            advancedNames.add(module.getName());
        }
        assertTrue(advancedNames.contains("Advanced Entity Randomizer"));
        assertTrue(advancedNames.contains("Enhanced Entity Randomizer"));
        assertTrue(advancedNames.contains("Table Of Lists Randomizer"));

        // Test an undefined group
        List<Module> nonExistentModules = wrapper.getModulesByGroup("nonexistent");
        assertNotNull(nonExistentModules);
        assertTrue(nonExistentModules.isEmpty());
    }

    @Test
    public void testExecuteModulesByGroup() {
        wrapper.loadModules();

        // Create test entity
        TestEntity entity = new TestEntity("Test Entity", 100, 50.0, true);

        // Create context with entity
        JavaContext context = new JavaContext();
        context.register("entity", entity);

        // Execute basic group module (simple_entity_randomizer)
        List<Module> basicModules = wrapper.getModulesByGroup("basic");
        assertEquals(1, basicModules.size());

        Map<String, Object> args = new HashMap<>();
        args.put("healthMin", 75);
        args.put("healthMax", 125);
        args.put("damageMultiplier", 1.5);

        ExecutionRequest request1 =
                ExecutionRequest.forModuleWithSeedOffset(basicModules.get(0).getId(), args, 0);
        ExecutionResult result1 = wrapper.executeModule(request1, context, TEST_BASE_SEED);
        assertTrue(result1.isSuccess());

        // Execute advanced group module (advanced_entity_randomizer)
        List<Module> advancedModules = wrapper.getModulesByGroup("advanced");
        assertEquals(3, advancedModules.size()); // Advanced, Enhanced, and Table Of Lists

        // Find and execute the advanced_entity_randomizer specifically
        Module advancedRandomizer = advancedModules.stream()
                .filter(m -> m.getId().equals("advanced_entity_randomizer")).findFirst()
                .orElse(null);
        assertNotNull(advancedRandomizer);

        Map<String, Object> args2 = new HashMap<>();
        args2.put("entityType", "mage");
        args2.put("level", 16);
        args2.put("applyBonus", false);

        ExecutionRequest request2 =
                ExecutionRequest.forModuleWithSeedOffset(advancedRandomizer.getId(), args2, 0);
        ExecutionResult result2 = wrapper.executeModule(request2, context, TEST_BASE_SEED);
        assertTrue(result2.isSuccess());
    }

    @Test
    public void testGetModulesByModifies() {
        wrapper.loadModules();

        // Get all modifies categories
        Set<String> modifies = wrapper.getDefinedModifiesValues();
        assertNotNull(modifies);
        assertTrue(modifies.contains("name"));
        assertTrue(modifies.contains("health"));
        assertTrue(modifies.contains("damage"));

        // Get modules that modify health
        List<Module> healthModules = wrapper.getModulesByModifies("health");
        assertNotNull(healthModules);
        assertEquals(3, healthModules.size());

        // Verify module names that modify health
        Set<String> healthModuleNames = new HashSet<>();
        for (Module module : healthModules) {
            healthModuleNames.add(module.getName());
        }
        assertTrue(healthModuleNames.contains("Simple Entity Randomizer"));
        assertTrue(healthModuleNames.contains("Advanced Entity Randomizer"));
        assertTrue(healthModuleNames.contains("Table Of Lists Randomizer"));

        // Test undefined modifies
        List<Module> nonExistentModules = wrapper.getModulesByModifies("nonexistent");
        assertNotNull(nonExistentModules);
        assertTrue(nonExistentModules.isEmpty());
    }

    @Test
    public void testExecuteModulesByModifies() {
        wrapper.loadModules();

        // Create test entity
        TestEntity entity = new TestEntity("Original Name", 100, 50.0, true);

        // Create context with entity
        JavaContext context = new JavaContext();
        context.register("entity", entity);

        // Get and execute modules that modify health
        List<Module> healthModules = wrapper.getModulesByModifies("health");
        assertEquals(3, healthModules.size());

        // Execute simple_entity_randomizer
        Module simpleRandomizer =
                healthModules.stream().filter(m -> m.getId().equals("simple_entity_randomizer"))
                        .findFirst().orElse(null);
        assertNotNull(simpleRandomizer);

        Map<String, Object> args1 = new HashMap<>();
        args1.put("healthMin", 150);
        args1.put("healthMax", 150);
        args1.put("damageMultiplier", 1.5);

        ExecutionRequest request1 =
                ExecutionRequest.forModuleWithSeedOffset(simpleRandomizer.getId(), args1, 0);
        ExecutionResult result1 = wrapper.executeModule(request1, context, TEST_BASE_SEED);
        assertTrue(result1.isSuccess());
        assertEquals(150, entity.getHealth());
        assertEquals(75.0, entity.getDamage());

        // Get and execute modules that modify name
        List<Module> nameModules = wrapper.getModulesByModifies("name");
        assertTrue(nameModules.size() >= 1);

        // Verify at least one module modifies the name
        Module advancedRandomizer =
                nameModules.stream().filter(m -> m.getId().equals("advanced_entity_randomizer"))
                        .findFirst().orElse(null);
        assertNotNull(advancedRandomizer);

        Map<String, Object> args2 = new HashMap<>();
        args2.put("entityType", "warrior");
        args2.put("level", 11);
        args2.put("applyBonus", true);

        ExecutionRequest request2 =
                ExecutionRequest.forModuleWithSeedOffset(advancedRandomizer.getId(), args2, 0);
        ExecutionResult result2 = wrapper.executeModule(request2, context, TEST_BASE_SEED);
        assertTrue(result2.isSuccess());
        assertNotEquals("Original Name", entity.getName());
    }
}

