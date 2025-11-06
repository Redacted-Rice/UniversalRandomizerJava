package redactedrice.randomizer.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PseudoEnumRegistryTest {

    private PseudoEnumRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new PseudoEnumRegistry();
    }

    @Test
    public void testRegisterEnum() {
        registry.registerEnum("TestEnum", "VALUE1", "VALUE2", "VALUE3");
        
        assertTrue(registry.hasRegistry("TestEnum"));
        assertTrue(registry.hasValue("TestEnum", "VALUE1"));
        assertTrue(registry.hasValue("TestEnum", "value1")); // Case insensitive
        assertTrue(registry.hasValue("TestEnum", "VALUE2"));
        assertTrue(registry.hasValue("TestEnum", "VALUE3"));
    }

    @Test
    public void testRegisterEnumNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerEnum(null, "VALUE1");
        });
    }

    @Test
    public void testRegisterEnumEmptyNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerEnum("", "VALUE1");
        });
    }

    @Test
    public void testRegisterEnumWhitespaceNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerEnum("   ", "VALUE1");
        });
    }

    @Test
    public void testRegisterEnumIgnoresNullValues() {
        registry.registerEnum("TestEnum", "VALUE1", null, "VALUE2", null);
        
        Set<String> values = registry.getValues("TestEnum");
        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    public void testRegisterEnumIgnoresEmptyValues() {
        registry.registerEnum("TestEnum", "VALUE1", "", "VALUE2", "   ");
        
        Set<String> values = registry.getValues("TestEnum");
        assertEquals(2, values.size());
    }

    @Test
    public void testRegisterEnumNormalizesValues() {
        registry.registerEnum("TestEnum", "  VALUE1  ", "VALUE2");
        
        assertTrue(registry.hasValue("TestEnum", "VALUE1"));
        assertTrue(registry.hasValue("TestEnum", "value1"));
        assertTrue(registry.hasValue("TestEnum", "  VALUE1  "));
    }

    @Test
    public void testRegisterEnumAddsToExisting() {
        registry.registerEnum("TestEnum", "VALUE1", "VALUE2");
        registry.registerEnum("TestEnum", "VALUE3", "VALUE4");
        
        Set<String> values = registry.getValues("TestEnum");
        assertEquals(4, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));
        assertTrue(values.contains("value4"));
    }

    @Test
    public void testExtendEnum() {
        registry.registerEnum("TestEnum", "VALUE1", "VALUE2");
        registry.extendEnum("TestEnum", "VALUE3", "VALUE4");
        
        Set<String> values = registry.getValues("TestEnum");
        assertEquals(4, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));
        assertTrue(values.contains("value4"));
    }

    @Test
    public void testExtendEnumCreatesIfNotExists() {
        registry.extendEnum("TestEnum", "VALUE1", "VALUE2");
        
        assertTrue(registry.hasRegistry("TestEnum"));
        assertTrue(registry.hasValue("TestEnum", "VALUE1"));
    }

    @Test
    public void testHasValue() {
        registry.registerEnum("TestEnum", "VALUE1", "VALUE2");
        
        assertTrue(registry.hasValue("TestEnum", "VALUE1"));
        assertTrue(registry.hasValue("TestEnum", "value1"));
        assertTrue(registry.hasValue("TestEnum", "  VALUE1  "));
        assertFalse(registry.hasValue("TestEnum", "INVALID"));
        assertFalse(registry.hasValue("TestEnum", null));
        assertFalse(registry.hasValue(null, "VALUE1"));
    }

    @Test
    public void testHasValueNonExistentRegistry() {
        assertFalse(registry.hasValue("NonExistent", "VALUE1"));
    }

    @Test
    public void testGetValues() {
        registry.registerEnum("TestEnum", "VALUE1", "VALUE2", "VALUE3");
        
        Set<String> values = registry.getValues("TestEnum");
        assertEquals(3, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));
    }

    @Test
    public void testGetValuesNonExistentRegistry() {
        Set<String> values = registry.getValues("NonExistent");
        assertTrue(values.isEmpty());
    }

    @Test
    public void testGetValuesReturnsUnmodifiableSet() {
        registry.registerEnum("TestEnum", "VALUE1");
        Set<String> values = registry.getValues("TestEnum");
        
        assertThrows(UnsupportedOperationException.class, () -> {
            values.add("VALUE2");
        });
    }

    @Test
    public void testGetRegistryNames() {
        registry.registerEnum("Enum1", "VALUE1");
        registry.registerEnum("Enum2", "VALUE1");
        registry.registerEnum("Enum3", "VALUE1");
        
        Set<String> names = registry.getRegistryNames();
        assertEquals(3, names.size());
        assertTrue(names.contains("Enum1"));
        assertTrue(names.contains("Enum2"));
        assertTrue(names.contains("Enum3"));
    }

    @Test
    public void testGetRegistryNamesReturnsUnmodifiableSet() {
        registry.registerEnum("Enum1", "VALUE1");
        Set<String> names = registry.getRegistryNames();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            names.add("Enum2");
        });
    }

    @Test
    public void testHasRegistry() {
        assertFalse(registry.hasRegistry("TestEnum"));
        registry.registerEnum("TestEnum", "VALUE1");
        assertTrue(registry.hasRegistry("TestEnum"));
    }

    @Test
    public void testClearRegistry() {
        registry.registerEnum("TestEnum", "VALUE1", "VALUE2", "VALUE3");
        assertEquals(3, registry.getValues("TestEnum").size());
        
        registry.clearRegistry("TestEnum");
        assertTrue(registry.getValues("TestEnum").isEmpty());
        assertTrue(registry.hasRegistry("TestEnum")); // Registry still exists
    }

    @Test
    public void testClearRegistryNonExistent() {
        // Should not throw
        registry.clearRegistry("NonExistent");
    }

    @Test
    public void testClearAll() {
        registry.registerEnum("Enum1", "VALUE1");
        registry.registerEnum("Enum2", "VALUE1");
        registry.registerEnum("Enum3", "VALUE1");
        
        assertEquals(3, registry.getRegistryNames().size());
        
        registry.clearAll();
        assertTrue(registry.getRegistryNames().isEmpty());
    }

    @Test
    public void testToString() {
        registry.registerEnum("Enum1", "VALUE1", "VALUE2");
        registry.registerEnum("Enum2", "VALUE3");
        
        String str = registry.toString();
        assertTrue(str.contains("PseudoEnumRegistry"));
        assertTrue(str.contains("Enum1"));
        assertTrue(str.contains("Enum2"));
    }

    @Test
    public void testMultipleRegistries() {
        registry.registerEnum("ModuleGroup", "gameplay", "visual");
        registry.registerEnum("ModuleModifies", "stats", "appearance");
        
        assertTrue(registry.hasRegistry("ModuleGroup"));
        assertTrue(registry.hasRegistry("ModuleModifies"));
        assertTrue(registry.hasValue("ModuleGroup", "gameplay"));
        assertTrue(registry.hasValue("ModuleModifies", "stats"));
        
        assertEquals(2, registry.getRegistryNames().size());
    }
}

