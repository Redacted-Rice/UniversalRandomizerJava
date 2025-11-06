package redactedrice.randomizer.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EnumRegistryTest {

    private EnumRegistry registry;

    @BeforeEach
    public void setUp() {
        // Use lowercase to match how predefined registries work
        registry = new EnumRegistry("TestRegistry", "value1", "value2", "value3");
    }

    @Test
    public void testConstructor() {
        assertEquals("TestRegistry", registry.getRegistryName());
        Set<String> coreValues = registry.getCoreValues();
        assertEquals(3, coreValues.size());
        // Core values are stored with original case
        assertTrue(coreValues.contains("value1"));
        assertTrue(coreValues.contains("value2"));
        assertTrue(coreValues.contains("value3"));
    }

    @Test
    public void testRegisterCustomValue() {
        registry.registerCustomValue("CUSTOM1", "Custom description");

        assertTrue(registry.isRegistered("CUSTOM1"));
        assertTrue(registry.isRegistered("custom1")); // Case insensitive
        Set<String> customValues = registry.getCustomValues();
        assertEquals(1, customValues.size());
        assertTrue(customValues.contains("custom1"));
    }

    @Test
    public void testRegisterCustomValueWithoutDescription() {
        registry.registerCustomValue("CUSTOM1");

        assertTrue(registry.isRegistered("CUSTOM1"));
        String description = registry.getDescription("CUSTOM1");
        assertNotNull(description);
        assertTrue(description.contains("Custom"));
    }

    @Test
    public void testRegisterCustomValueWithDescription() {
        registry.registerCustomValue("CUSTOM1", "My custom description");

        assertEquals("My custom description", registry.getDescription("CUSTOM1"));
    }

    @Test
    public void testRegisterCustomValueWithNullDescription() {
        registry.registerCustomValue("CUSTOM1", null);

        String description = registry.getDescription("CUSTOM1");
        assertNotNull(description);
        assertTrue(description.contains("Custom"));
    }

    @Test
    public void testRegisterCustomValueWithEmptyDescription() {
        registry.registerCustomValue("CUSTOM1", "");

        String description = registry.getDescription("CUSTOM1");
        assertNotNull(description);
        assertTrue(description.contains("Custom"));
    }

    @Test
    public void testRegisterCustomValueNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerCustomValue(null);
        });
    }

    @Test
    public void testRegisterCustomValueEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerCustomValue("");
        });
    }

    @Test
    public void testRegisterCustomValueWhitespaceThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerCustomValue("   ");
        });
    }

    @Test
    public void testRegisterCoreValueAsCustom() {
        // Registering a core value as custom should not add it to custom values
        registry.registerCustomValue("value1");

        Set<String> customValues = registry.getCustomValues();
        // Since value1 is already a core value (normalized check), it won't be added to custom
        assertFalse(customValues.contains("value1"));
        assertTrue(registry.isRegistered("value1")); // Should still be registered as core
    }

    @Test
    public void testIsRegistered() {
        assertTrue(registry.isRegistered("value1"));
        assertTrue(registry.isRegistered("VALUE1")); // Case insensitive
        assertTrue(registry.isRegistered("VaLuE1")); // Case insensitive
        assertFalse(registry.isRegistered("invalid"));
        assertFalse(registry.isRegistered(null));
    }

    @Test
    public void testGetAllValues() {
        Set<String> allValues = registry.getAllValues();
        assertEquals(3, allValues.size());
        // Core values are stored with original case
        assertTrue(allValues.contains("value1"));
        assertTrue(allValues.contains("value2"));
        assertTrue(allValues.contains("value3"));

        registry.registerCustomValue("CUSTOM1");
        allValues = registry.getAllValues();
        assertEquals(4, allValues.size());
        // Custom values are normalized to lowercase when stored
        assertTrue(allValues.contains("custom1"));
    }

    @Test
    public void testGetCoreValues() {
        Set<String> coreValues = registry.getCoreValues();
        assertEquals(3, coreValues.size());
        assertTrue(coreValues.contains("value1"));

        registry.registerCustomValue("CUSTOM1");
        // Core values should not change
        assertEquals(3, registry.getCoreValues().size());
    }

    @Test
    public void testGetCustomValues() {
        Set<String> customValues = registry.getCustomValues();
        assertTrue(customValues.isEmpty());

        registry.registerCustomValue("CUSTOM1");
        registry.registerCustomValue("CUSTOM2");
        customValues = registry.getCustomValues();
        assertEquals(2, customValues.size());
        assertTrue(customValues.contains("custom1"));
        assertTrue(customValues.contains("custom2"));
    }

    @Test
    public void testGetDescription() {
        // Descriptions are stored with original case but lookup normalizes to lowercase
        // Since we're using lowercase values, this should work
        assertEquals("Core TestRegistry value", registry.getDescription("value1"));
        assertNull(registry.getDescription("invalid"));
        assertNull(registry.getDescription(null));

        registry.registerCustomValue("CUSTOM1", "Custom description");
        // Custom values are normalized, so this should work
        assertEquals("Custom description", registry.getDescription("CUSTOM1"));
    }

    @Test
    public void testClearCustomValues() {
        registry.registerCustomValue("CUSTOM1");
        registry.registerCustomValue("CUSTOM2");
        assertEquals(2, registry.getCustomValues().size());

        registry.clearCustomValues();
        assertTrue(registry.getCustomValues().isEmpty());
        assertEquals(3, registry.getCoreValues().size()); // Core values unchanged
    }

    @Test
    public void testToString() {
        String str = registry.toString();
        assertTrue(str.contains("TestRegistry"));
        assertTrue(str.contains("3 values"));
        assertTrue(str.contains("3 core"));
        assertTrue(str.contains("0 custom"));

        registry.registerCustomValue("CUSTOM1");
        str = registry.toString();
        assertTrue(str.contains("4 values"));
        assertTrue(str.contains("1 custom"));
    }

    @Test
    public void testGetModuleGroups() {
        EnumRegistry moduleGroups = EnumRegistry.getModuleGroups();
        assertNotNull(moduleGroups);
        assertEquals("ModuleGroup", moduleGroups.getRegistryName());
        assertTrue(moduleGroups.isRegistered("gameplay"));
        assertTrue(moduleGroups.isRegistered("visual"));
        assertTrue(moduleGroups.isRegistered("audio"));
        assertTrue(moduleGroups.isRegistered("balance"));
        assertTrue(moduleGroups.isRegistered("content"));
        assertTrue(moduleGroups.isRegistered("utility"));
        assertTrue(moduleGroups.isRegistered("experimental"));
    }

    @Test
    public void testGetModuleModifies() {
        EnumRegistry moduleModifies = EnumRegistry.getModuleModifies();
        assertNotNull(moduleModifies);
        assertEquals("ModuleModifies", moduleModifies.getRegistryName());
        assertTrue(moduleModifies.isRegistered("stats"));
        assertTrue(moduleModifies.isRegistered("appearance"));
        assertTrue(moduleModifies.isRegistered("behavior"));
        assertTrue(moduleModifies.isRegistered("loot"));
        assertTrue(moduleModifies.isRegistered("difficulty"));
        assertTrue(moduleModifies.isRegistered("progression"));
        assertTrue(moduleModifies.isRegistered("economy"));
        assertTrue(moduleModifies.isRegistered("environment"));
    }

    @Test
    public void testValueNormalization() {
        // Test that values are normalized to lowercase
        registry.registerCustomValue("  CUSTOM_VALUE  ", "Description");

        assertTrue(registry.isRegistered("custom_value"));
        assertTrue(registry.isRegistered("CUSTOM_VALUE"));
        assertTrue(registry.isRegistered("  custom_value  "));

        String description = registry.getDescription("  CUSTOM_VALUE  ");
        assertNotNull(description);
    }
}

