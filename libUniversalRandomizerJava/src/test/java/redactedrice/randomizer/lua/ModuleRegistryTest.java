package redactedrice.randomizer.lua;

import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.LuaRandomizerWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for module registry grouping and loading functionality
 */
public class ModuleRegistryTest {

    private LuaRandomizerWrapper wrapper;
    private String randomizerPath;
    private String modulesPath;

    @BeforeEach
    public void setUp() {
        randomizerPath = new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        modulesPath = new File("src/test/java/redactedrice/support/module_registry_test_modules")
                .getAbsolutePath();

        List<String> allowedDirectories = Arrays.asList(randomizerPath, modulesPath);
        List<String> searchPaths = Arrays.asList(modulesPath);

        wrapper = new LuaRandomizerWrapper(allowedDirectories, searchPaths);
    }

    @Test
    public void testSetDefinedGroups() {
        // Create wrapper with defined groups filter
        Set<String> definedGroups = new HashSet<>();
        definedGroups.add("health");

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), definedGroups);

        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0);

        // Only health group modules should be loaded
        List<Module> healthModules = wrapper.getModulesByGroup("health");
        assertEquals(2, healthModules.size());

        // Damage group should be empty (was filtered out)
        List<Module> damageModules = wrapper.getModulesByGroup("damage");
        assertEquals(0, damageModules.size());

        // Verify specific modules
        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("health_randomizer"));
        assertTrue(moduleIds.contains("health_booster"));
        assertFalse(moduleIds.contains("damage_randomizer"));
    }

    @Test
    public void testDefinedGroupsFilterBySecondaryGroupTag() {
        // Create wrapper with defined groups filter
        Set<String> definedGroups = new HashSet<>();
        definedGroups.add("damage");

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), definedGroups);

        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0);

        // Verify specific modules
        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("damage_randomizer"));
        assertFalse(moduleIds.contains("health_booster"));
    }

    @Test
    public void testDefinedGroupsRequiresMatchingTag() {
        // Create wrapper with defined groups filter
        Set<String> definedGroups = new HashSet<>();
        definedGroups.add("stats");

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), definedGroups);

        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0);

        // Verify specific modules
        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("health_randomizer"));
        assertFalse(moduleIds.contains("health_booster"));
        assertTrue(moduleIds.contains("damage_randomizer"));
    }

    @Test
    public void testNullDefinedGroupsAllowAll() {
        // Create wrapper with null to dynamically define the groups
        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null);

        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0);

        // All modules should be loaded
        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("health_randomizer"));
        assertTrue(moduleIds.contains("health_booster"));
        assertTrue(moduleIds.contains("damage_randomizer"));
    }

    @Test
    public void testGetDefinedGroupValues() {
        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), Set.of("health", "damage"));
        wrapper.loadModules();

        Set<String> presetGroups = wrapper.getDefinedGroupValues();
        assertEquals(2, presetGroups.size());
        assertTrue(presetGroups.contains("health"));
        assertTrue(presetGroups.contains("damage"));

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null);
        wrapper.loadModules();
        Set<String> loadedGroups = wrapper.getDefinedGroupValues();
        assertTrue(loadedGroups.contains("health"));
        assertTrue(loadedGroups.contains("damage"));
        assertTrue(loadedGroups.contains("stats"));
    }

    @Test
    public void testGroupFilterOnlyAddsToDefinedCategories() {
        Set<String> definedGroups = new HashSet<>(Arrays.asList("health"));

        LuaRandomizerWrapper wrapper =
                new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                        Arrays.asList(modulesPath), definedGroups);

        wrapper.loadModules();

        // Health Randomizer has health and stats. Since stats is not defined
        // it should only be loaded in the health module
        Module module = wrapper.getModule("health_randomizer");
        assertNotNull(module);

        // It should appear in health category
        List<Module> healthModules = wrapper.getModulesByGroup("health");
        assertTrue(healthModules.stream().anyMatch(m -> m.getName().equals("Health Randomizer")));

        // It should not appear in stats category since it was not defined ahead of time
        List<Module> statsModules = wrapper.getModulesByGroup("stats");
        assertFalse(statsModules.stream().anyMatch(m -> m.getName().equals("Health Randomizer")));
    }

    @Test
    public void testMinimalGroupModuleLoads() {
        wrapper.loadModules();
        Module module = wrapper.getModule("minimal_groups_test");
        assertNotNull(module);
        assertFalse(module.getGroups().isEmpty());
    }
}
