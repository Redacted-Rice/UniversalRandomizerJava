package redactedrice.randomizer;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.wrapper.RandomizerResourceExtractor;
import redactedrice.support.test.TestEntity;
import redactedrice.randomizer.wrapper.LuaRandomizerWrapper;
import redactedrice.randomizer.wrapper.ExecutionResult;
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
    public void testExecuteModuleWithArguments() {
        wrapper.loadModules();
        wrapper.setChangeDetectionEnabled(true);

        TestEntity entity = new TestEntity("Original", 100, 10.0, true);
        wrapper.setMonitoredObjects(entity);

        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("healthMin", 50);
        arguments.put("healthMax", 200);
        arguments.put("damageMultiplier", 1.5);

        ExecutionResult result =
                wrapper.executeModule("Simple Entity Randomizer", context, arguments);

        assertTrue(result.isSuccess());
        assertNotEquals("Original", entity.getName());
        assertTrue(entity.getHealth() >= 50 && entity.getHealth() <= 200);
        assertEquals(15.0, entity.getDamage(), 0.01);
        assertTrue(result.hasChanges());
    }

    @Test
    public void testExecuteModuleWithComplexArguments() {
        wrapper.loadModules();
        wrapper.setChangeDetectionEnabled(true);

        TestEntity entity = new TestEntity("Original", 100, 10.0, true);
        wrapper.setMonitoredObjects(entity);

        JavaContext context = new JavaContext();
        context.register("entity", entity);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("entityType", "warrior");
        arguments.put("level", 11);
        arguments.put("applyBonus", true);

        ExecutionResult result =
                wrapper.executeModule("Advanced Entity Randomizer", context, arguments);

        assertTrue(result.isSuccess());
        assertNotEquals("Original", entity.getName());
        assertTrue(entity.getHealth() > 100);
        assertTrue(entity.getDamage() > 10.0);
        assertTrue(result.hasChanges());
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
        wrapper.executeModule("Simple Entity Randomizer", context1, args, 999);

        TestEntity entity2 = new TestEntity("Hero", 100, 10.0, true);
        JavaContext context2 = new JavaContext();
        context2.register("entity", entity2);
        wrapper.executeModule("Simple Entity Randomizer", context2, args, 999);

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
        wrapper.executeModule("Simple Entity Randomizer", context1, args1);

        JavaContext context2 = new JavaContext();
        context2.register("entity", entity2);
        Map<String, Object> args2 = new HashMap<>();
        args2.put("entityType", "mage");
        args2.put("level", 16);
        args2.put("applyBonus", false);
        wrapper.executeModule("Advanced Entity Randomizer", context2, args2);

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

        ExecutionResult result =
                wrapper.executeModule("Advanced Entity Randomizer", context, badArgs);
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

        wrapper.executeModule("Simple Entity Randomizer", context, args);

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

        ExecutionResult result =
                wrapper.executeModule("Simple Entity Randomizer", emptyContext, args);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
}

