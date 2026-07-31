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
                Arrays.asList(modulesPath), definedGroups, null);

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
    public void testSetDefinedModifies() {
        // Create wrapper with defined modifies filter
        Set<String> definedModifies = new HashSet<>();
        definedModifies.add("damage");

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null, definedModifies);

        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0);

        // Only modules that modify damage should be loaded
        List<Module> damageModules = wrapper.getModulesByModifies("damage");
        assertTrue(damageModules.size() > 0);

        // Verify specific modules
        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("damage_randomizer"));
        // Health Booster only modifies health so it should not be here
        assertFalse(moduleIds.contains("health_booster"));
    }

    @Test
    public void testCombinedDefinedGroupAndModifies() {
        // Create wrapper with both defined groups and modifies filters
        Set<String> definedGroups = new HashSet<>();
        definedGroups.add("health");

        Set<String> definedModifies = new HashSet<>();
        definedModifies.add("stats");

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), definedGroups, definedModifies);

        int loaded = wrapper.loadModules();
        assertTrue(loaded > 0);

        // Only modules in health group and with stats modifies should be loaded
        Set<String> moduleIds = wrapper.getModuleIds();
        assertTrue(moduleIds.contains("health_randomizer")); // health group + stats modifies
        assertFalse(moduleIds.contains("health_booster")); // health group but no stats modifies
        assertFalse(moduleIds.contains("damage_randomizer")); // stats modifies but damage group
    }

    @Test
    public void testNullDefinedGroupsAndModifiesAllowAll() {
        // Create wrapper with null to dynamically define the groups/modifies
        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null, null);

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
                Arrays.asList(modulesPath), Set.of("health", "damage"), null);
        wrapper.loadModules();

        Set<String> presetGroups = wrapper.getDefinedGroupValues();
        assertEquals(2, presetGroups.size());
        assertTrue(presetGroups.contains("health"));
        assertTrue(presetGroups.contains("damage"));

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null, null);
        wrapper.loadModules();
        Set<String> loadedGroups = wrapper.getDefinedGroupValues();
        assertTrue(loadedGroups.contains("health"));
        assertTrue(loadedGroups.contains("damage"));
    }

    @Test
    public void testGetDefinedModifiesValues() {
        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null, Set.of("health", "stats", "damage"));
        wrapper.loadModules();

        Set<String> presetModifies = wrapper.getDefinedModifiesValues();
        assertEquals(3, presetModifies.size());
        assertTrue(presetModifies.contains("health"));
        assertTrue(presetModifies.contains("stats"));
        assertTrue(presetModifies.contains("damage"));

        wrapper = new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                Arrays.asList(modulesPath), null, null);
        wrapper.loadModules();
        Set<String> loadedModifies = wrapper.getDefinedModifiesValues();
        assertTrue(loadedModifies.contains("health"));
        assertTrue(loadedModifies.contains("damage"));
        assertTrue(loadedModifies.contains("stats"));
    }

    @Test
    public void testModifiesFilterOnlyAddsToDefinedCategories() {
        // Health Randomizer modifies health and stats. Since stats is not defined
        // it should only be loaded in the health module
        Set<String> definedModifies = new HashSet<>(Arrays.asList("health"));

        LuaRandomizerWrapper wrapper =
                new LuaRandomizerWrapper(Arrays.asList(randomizerPath, modulesPath),
                        Arrays.asList(modulesPath), null, definedModifies);

        wrapper.loadModules();

        // Health Randomizer should be loaded since it has health modifies
        Module module = wrapper.getModule("health_randomizer");
        assertNotNull(module);

        // It should appear in health category
        List<Module> healthModules = wrapper.getModulesByModifies("health");
        assertTrue(healthModules.stream().anyMatch(m -> m.getName().equals("Health Randomizer")));

        // It should not appear in stats category since it was not defined ahead of time
        List<Module> statsModules = wrapper.getModulesByModifies("stats");
        assertFalse(statsModules.stream().anyMatch(m -> m.getName().equals("Health Randomizer")));
    }

    @Test
    public void testModifiesFieldIsOptional() {
        // Modules should allow empty/missing modifies
        wrapper.loadModules();
        Module module = wrapper.getModule("no_modifies_test");
        assertNotNull(module);
        assertTrue(module.getModifies().isEmpty());
        assertFalse(module.getGroups().isEmpty());
    }
}
