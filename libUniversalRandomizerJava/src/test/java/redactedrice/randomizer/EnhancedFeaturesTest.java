package redactedrice.randomizer;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.context.EnumRegistry;
import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.lua.arguments.*;
import redactedrice.support.test.TestEntity;
import redactedrice.support.test.EntityType;
import redactedrice.support.test.FlagEnum;
import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for enhanced features - similar to appExample. Tests end-to-end functionality
 * with enums, complex types, and module execution.
 */
public class EnhancedFeaturesTest {

    private static final int TEST_BASE_SEED = 12345;

    private LuaRandomizerWrapper wrapper;
    private String testModulesPath;

    @BeforeEach
    public void setup() {
        String randomizerPath = new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        testModulesPath =
                new File("src/test/java/redactedrice/support/lua_modules").getAbsolutePath();

        // Define allowed directories: randomizer + modules
        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(randomizerPath);
        allowedDirectories.add(testModulesPath);

        // Search paths for module discovery
        List<String> searchPaths = new ArrayList<>();
        searchPaths.add(testModulesPath);

        wrapper = new LuaRandomizerWrapper(allowedDirectories, searchPaths);

        // Register EntityType enum in shared context before loading modules
        // This allows enum_expansion_test.lua to expand it during onLoad
        wrapper.getSharedContext().registerEnum("EntityType", EntityType.class);

        wrapper.loadModules();
    }

    @Test
    public void testExecuteModuleWithEnumArguments() {
        TestEntity entity = new TestEntity("Test", 100, 10.0, true);

        JavaContext context = new JavaContext();
        context.register("entity", entity);
        context.registerEnum("EntityType", EntityType.class);

        Map<String, Object> args = new HashMap<>();
        args.put("entityType", "WARRIOR");
        args.put("statBonuses", Arrays.asList(20, 5, 0));

        Map<String, Integer> modifiers = new HashMap<>();
        modifiers.put("health", 30);
        modifiers.put("damage", 10);
        args.put("attributeModifiers", modifiers);
        args.put("applyRandomness", true);
        args.put("powerLevel", 75);

        ExecutionRequest request = ExecutionRequest
                .forModuleWithSeedOffset("enhanced_entity_randomizer", args, 0);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);

        assertTrue(result.isSuccess(), "Execution should succeed: " + result.getErrorMessage());
        assertNotEquals("Test", entity.getName());
        assertTrue(entity.getHealth() > 100);
        assertTrue(entity.getDamage() > 10.0);
    }

    @Test
    public void testModuleGrouping() {
        List<Module> modules = wrapper.getAvailableModules();
        Map<String, List<String>> modulesByGroup = new HashMap<>();
        for (Module module : modules) {
            for (String group : module.getGroups()) {
                modulesByGroup.putIfAbsent(group, new ArrayList<>());
                modulesByGroup.get(group).add(module.getName());
            }
        }

        assertTrue(modulesByGroup.containsKey("gameplay"));
        assertTrue(modulesByGroup.get("gameplay").contains("Enhanced Entity Randomizer"));
    }

    private static class TestEntityWithFlagEnum {
        private FlagEnum flag;

        public FlagEnum getFlag() {
            return flag;
        }

        public void setFlag(FlagEnum flag) {
            this.flag = flag;
        }
    }

    private static class TestEntityWithCustomEnum {
        private String priority;

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    @Test
    public void testEnumRegistrationInOnLoad() {
        // Load modules (this will call onLoad functions)
        wrapper.loadModules();

        // Check that the enum_onload module was loaded
        Module onLoadModule = wrapper.getModule("enum_onload");
        assertNotNull(onLoadModule, "enum_onload module should be loaded");
        assertTrue(onLoadModule.hasOnLoad(), "enum_onload module should have onLoad function");

        // Check that the enum_usage module was loaded
        Module usageModule = wrapper.getModule("enum_usage");
        assertNotNull(usageModule, "enum_usage module should be loaded");

        // Create a context and execute the usage module
        // The enum registered in onLoad should be available
        JavaContext context = new JavaContext();

        // Execute the usage module - it should be able to access TestPriority enum
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("enum_usage", new HashMap<>(), 0);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);

        assertTrue(result.isSuccess(),
                "Module execution should succeed: " + result.getErrorMessage());

        // Verify all three enum types are available in the context after execution
        EnumRegistry enumContext = context.getEnumRegistry();

        // Test TestPriority (case 1: array with explicit values subtable)
        assertTrue(enumContext.hasEnum("TestPriority"), "TestPriority enum should be registered");
        EnumDefinition testPriorityDef = enumContext.getEnum("TestPriority");
        assertNotNull(testPriorityDef);
        assertEquals(1, testPriorityDef.getValue("LOW").intValue());
        assertEquals(50, testPriorityDef.getValue("MEDIUM").intValue());
        assertEquals(100, testPriorityDef.getValue("HIGH").intValue());

        // Test TestPriority2 (case 2: array with implicit values 0, 1, 2)
        assertTrue(enumContext.hasEnum("TestPriority2"), "TestPriority2 enum should be registered");
        EnumDefinition testPriority2Def = enumContext.getEnum("TestPriority2");
        assertNotNull(testPriority2Def);
        assertEquals(0, testPriority2Def.getValue("LOW").intValue());
        assertEquals(1, testPriority2Def.getValue("MEDIUM").intValue());
        assertEquals(2, testPriority2Def.getValue("HIGH").intValue());

        // Test TestPriority3 (case 3: map-based enum)
        assertTrue(enumContext.hasEnum("TestPriority3"), "TestPriority3 enum should be registered");
        EnumDefinition testPriority3Def = enumContext.getEnum("TestPriority3");
        assertNotNull(testPriority3Def);
        assertEquals(1, testPriority3Def.getValue("LOW").intValue());
        assertEquals(50, testPriority3Def.getValue("MEDIUM").intValue());
        assertEquals(100, testPriority3Def.getValue("HIGH").intValue());
    }

    @Test
    public void testFlagEnumInModules() {
        // Load modules
        wrapper.loadModules();

        // Create a test entity with FlagEnum field
        TestEntityWithFlagEnum testEntity = new TestEntityWithFlagEnum();

        // Create a context for execution and register FlagEnum in it
        JavaContext execContext = new JavaContext();
        execContext.registerEnum("FlagEnum", FlagEnum.class);
        execContext.register("testEntity", testEntity);

        // Execute the flag_enum module
        // The wrapper will merge the shared enum context (from onLoad) into execContext,
        // and FlagEnum will already be in execContext
        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("flag_enum", new HashMap<>(), 0);
        ExecutionResult result = wrapper.executeModule(request, execContext, TEST_BASE_SEED);

        assertTrue(result.isSuccess(),
                "Module execution should succeed: " + result.getErrorMessage());

        // Verify FlagEnum is available in the context
        EnumRegistry enumContext = execContext.getEnumRegistry();
        assertTrue(enumContext.hasEnum("FlagEnum"), "FlagEnum should be registered");

        EnumDefinition flagEnumDef = enumContext.getEnum("FlagEnum");
        assertNotNull(flagEnumDef);

        // Verify flag values were extracted correctly (from getValue() method, not
        // ordinals)
        assertEquals(0, flagEnumDef.getValue("FLAG_NONE").intValue());
        assertEquals(1, flagEnumDef.getValue("FLAG_ONE").intValue());
        assertEquals(2, flagEnumDef.getValue("FLAG_TWO").intValue());
        assertEquals(4, flagEnumDef.getValue("FLAG_FOUR").intValue()); // Non-sequential!
        assertEquals(8, flagEnumDef.getValue("FLAG_EIGHT").intValue()); // Non-sequential!

        // Verify that the enum value was correctly converted from Lua string to Java enum
        assertNotNull(testEntity.getFlag(), "FlagEnum value should have been set by Lua module");
        assertEquals(FlagEnum.FLAG_FOUR, testEntity.getFlag(),
                "FlagEnum should be FLAG_FOUR (converted from string 'FLAG_FOUR' in Lua)");
    }

    @Test
    public void testEnumFromOnLoadAvailableInJava() {
        // Load modules (this will call onLoad functions)
        wrapper.loadModules();

        // Create a test entity with TestPriority field
        TestEntityWithCustomEnum testEntity = new TestEntityWithCustomEnum();

        // Create a context and execute a module that uses the enum
        JavaContext context = new JavaContext();
        context.register("testEntity", testEntity);

        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("enum_usage", new HashMap<>(), 0);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);

        assertTrue(result.isSuccess(),
                "Module execution should succeed: " + result.getErrorMessage());

        // Verify the enum is available in Java after execution
        EnumRegistry enumContext = context.getEnumRegistry();
        assertTrue(enumContext.hasEnum("TestPriority"),
                "TestPriority enum should be available in Java");

        EnumDefinition testPriorityDef = enumContext.getEnum("TestPriority");
        assertNotNull(testPriorityDef);

        // Verify the enum values match what was registered in onLoad
        assertTrue(testPriorityDef.hasValue("LOW"));
        assertTrue(testPriorityDef.hasValue("MEDIUM"));
        assertTrue(testPriorityDef.hasValue("HIGH"));
        assertEquals(1, testPriorityDef.getValue("LOW").intValue());
        assertEquals(50, testPriorityDef.getValue("MEDIUM").intValue());
        assertEquals(100, testPriorityDef.getValue("HIGH").intValue());

        // Verify that the enum value was correctly passed from Lua to Java
        assertNotNull(testEntity.getPriority(),
                "TestPriority value should have been set by Lua module");
        assertEquals("MEDIUM", testEntity.getPriority(),
                "TestPriority should be 'MEDIUM' (passed as string from Lua)");
    }

    @Test
    public void testEnumExtensionFromJava() {
        // Create a new registry for this test because the original enum will already be extended
        // by the lua version of this test on loading the file
        EnumRegistry testRegistry = new EnumRegistry();
        testRegistry.registerEnum("EntityType", EntityType.class);

        // Verify initial enum has only the original Java values
        EnumDefinition initialEnumDef = testRegistry.getEnum("EntityType");
        assertNotNull(initialEnumDef);
        assertEquals(5, initialEnumDef.getValues().size());
        assertTrue(initialEnumDef.hasValue("WARRIOR"));
        assertTrue(initialEnumDef.hasValue("MAGE"));
        assertTrue(initialEnumDef.hasValue("ROGUE"));
        assertTrue(initialEnumDef.hasValue("CLERIC"));
        assertTrue(initialEnumDef.hasValue("RANGER"));
        // Shouldn't exist yet
        assertFalse(initialEnumDef.hasValue("PALADIN"));
        assertFalse(initialEnumDef.hasValue("NECROMANCER"));

        // Extend the enum using the extendEnum method
        testRegistry.extendEnum("EntityType", Arrays.asList("PALADIN", "NECROMANCER"),
                Map.of("PALADIN", 100, "NECROMANCER", 101));

        // Verify the enum was extended
        EnumDefinition extendedEnumDef = testRegistry.getEnum("EntityType");
        assertNotNull(extendedEnumDef);
        assertEquals(7, extendedEnumDef.getValues().size());

        // Original values should still exist
        assertTrue(extendedEnumDef.hasValue("WARRIOR"));
        assertTrue(extendedEnumDef.hasValue("MAGE"));
        assertTrue(extendedEnumDef.hasValue("ROGUE"));
        assertTrue(extendedEnumDef.hasValue("CLERIC"));
        assertTrue(extendedEnumDef.hasValue("RANGER"));

        // New extended values should exist
        assertTrue(extendedEnumDef.hasValue("PALADIN"));
        assertTrue(extendedEnumDef.hasValue("NECROMANCER"));

        // Verify the custom value mappings
        assertEquals(100, extendedEnumDef.getValue("PALADIN").intValue());
        assertEquals(101, extendedEnumDef.getValue("NECROMANCER").intValue());

        // Verify the values list maintains the expected order
        List<String> allValues = extendedEnumDef.getValues();
        assertEquals(Arrays.asList("WARRIOR", "MAGE", "ROGUE", "CLERIC", "RANGER", "PALADIN",
                "NECROMANCER"), allValues);
    }

    @Test
    public void testEnumExtensionFromLua() {
        EnumRegistry sharedRegistry = wrapper.getSharedContext().getEnumRegistry();
        EnumDefinition entityTypeDef = sharedRegistry.getEnum("EntityType");

        // Enum is extended on load which already occurred by the time this test runs
        assertNotNull(entityTypeDef);
        assertEquals(7, entityTypeDef.getValues().size());

        assertTrue(entityTypeDef.hasValue("WARRIOR"));
        assertTrue(entityTypeDef.hasValue("MAGE"));
        assertTrue(entityTypeDef.hasValue("ROGUE"));
        assertTrue(entityTypeDef.hasValue("CLERIC"));
        assertTrue(entityTypeDef.hasValue("RANGER"));
        assertTrue(entityTypeDef.hasValue("PALADIN"));
        assertTrue(entityTypeDef.hasValue("NECROMANCER"));
        assertEquals(100, entityTypeDef.getValue("PALADIN").intValue());
        assertEquals(101, entityTypeDef.getValue("NECROMANCER").intValue());

        // Now execute the module with an extended enum value
        TestEntity entity = new TestEntity("TestEntity", 100, 10.0, false);
        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> args = new HashMap<>();
        args.put("entityType", "PALADIN");

        ExecutionRequest request =
                ExecutionRequest.forModuleWithSeedOffset("enum_expansion_test", args, 0);
        ExecutionResult result = wrapper.executeModule(request, context, TEST_BASE_SEED);

        // Will fail an assert and not return true if it does not work
        assertTrue(result.isSuccess());
    }

    @Test
    public void testExecuteSameModuleTwiceWithDifferentSeedsAndArgs() {
        // Create two requests for the same module with different args and seeds
        Map<String, Object> args1 = new HashMap<>();
        args1.put("healthMin", 50);
        args1.put("healthMax", 100);
        args1.put("damageMultiplier", 1.5);
        ExecutionRequest request1 = ExecutionRequest.forModuleWithSeedOffset(
                "simple_entity_randomizer", args1, 11111 - TEST_BASE_SEED);

        Map<String, Object> args2 = new HashMap<>();
        args2.put("healthMin", 80);
        args2.put("healthMax", 120);
        args2.put("damageMultiplier", 2.0);
        ExecutionRequest request2 = ExecutionRequest.forModuleWithSeedOffset(
                "simple_entity_randomizer", args2, 22222 - TEST_BASE_SEED);

        // Execute the modules in batch
        TestEntity entity1 = new TestEntity("Hero", 100, 10.0, true);
        JavaContext context = new JavaContext();
        context.register("entity", entity1);
        List<ExecutionRequest> requests = Arrays.asList(request1, request2);
        List<ExecutionResult> batchResults =
                wrapper.executeModules(requests, context, TEST_BASE_SEED);

        // Verify batch execution results
        assertEquals(2, batchResults.size());
        assertTrue(batchResults.get(0).isSuccess());
        assertTrue(batchResults.get(1).isSuccess());

        // Verify that the seeds used match what we requested
        assertEquals(11111, batchResults.get(0).getSeedUsed(),
                "First execution should use seed 11111");
        assertEquals(22222, batchResults.get(1).getSeedUsed(),
                "Second execution should use seed 22222");

        // Verify that the arguments match what we requested via getRequest()
        ExecutionRequest resultRequest1 = batchResults.get(0).getRequest();
        ExecutionRequest resultRequest2 = batchResults.get(1).getRequest();

        assertEquals(request1, resultRequest1);
        assertEquals(request2, resultRequest2);
    }
}

