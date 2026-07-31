package redactedrice.randomizer.context;

import redactedrice.randomizer.context.testsupport.TestEnumWithValue;
import redactedrice.randomizer.context.testsupport.RegistryTestEnum;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EnumRegistryTest {

    @Test
    public void testRegisterEnumFromClass() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum(RegistryTestEnum.class);

        assertTrue(context.hasEnum("RegistryTestEnum"));
        EnumDefinition def = context.getEnum("RegistryTestEnum");
        assertNotNull(def);
        assertTrue(def.hasValue("VALUE1"));
        assertTrue(def.hasValue("VALUE2"));
        assertTrue(def.hasValue("VALUE3"));
    }

    @Test
    public void testRegisterEnumWithCustomName() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("CustomName", RegistryTestEnum.class);

        assertTrue(context.hasEnum("CustomName"));
        assertFalse(context.hasEnum("RegistryTestEnum"));
    }

    @Test
    public void testRegisterEnumWithValues() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"), null);

        assertTrue(context.hasEnum("Difficulty"));
        EnumDefinition def = context.getEnum("Difficulty");
        assertTrue(def.hasValue("EASY"));
        assertTrue(def.hasValue("NORMAL"));
        assertTrue(def.hasValue("HARD"));
    }

    @Test
    public void testRegisterEnumWithValueMap() {
        EnumRegistry context = new EnumRegistry();
        Map<String, Integer> valueMap = new LinkedHashMap<>();
        valueMap.put("LOW", 1);
        valueMap.put("MEDIUM", 10);
        valueMap.put("HIGH", 100);
        context.registerEnum("Priority", valueMap);

        EnumDefinition def = context.getEnum("Priority");
        assertEquals(1, def.getValue("LOW").intValue());
        assertEquals(10, def.getValue("MEDIUM").intValue());
        assertEquals(100, def.getValue("HIGH").intValue());
    }

    @Test
    public void testEnumWithValueProvider() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("Priority", TestEnumWithValue.class);

        EnumDefinition def = context.getEnum("Priority");
        assertEquals(1, def.getValue("LOW").intValue());
        assertEquals(10, def.getValue("MEDIUM").intValue());
        assertEquals(100, def.getValue("HIGH").intValue());
    }

    @Test
    public void testStringToEnum() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("RegistryTestEnum", RegistryTestEnum.class);

        Object result = context.stringToEnum("RegistryTestEnum", "VALUE1");
        assertEquals(RegistryTestEnum.VALUE1, result);

        Object result2 = context.stringToEnum("RegistryTestEnum", "INVALID");
        assertNull(result2);
    }

    @Test
    public void testIsValidEnumValue() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        assertTrue(context.isValidEnumValue("Difficulty", "EASY"));
        assertTrue(context.isValidEnumValue("Difficulty", "NORMAL"));
        assertFalse(context.isValidEnumValue("Difficulty", "INVALID"));
    }

    @Test
    public void testMergeFrom() {
        EnumRegistry source = new EnumRegistry();
        source.registerEnum("Enum1", Arrays.asList("A", "B"));

        EnumRegistry target = new EnumRegistry();
        target.registerEnum("Enum2", Arrays.asList("C", "D"));

        target.mergeFrom(source);
        assertTrue(target.hasEnum("Enum1"));
        assertTrue(target.hasEnum("Enum2"));
    }

    @Test
    public void testToLuaTables() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        Map<String, org.luaj.vm2.LuaTable> tables = context.toLuaTables();
        assertTrue(tables.containsKey("Difficulty"));

        org.luaj.vm2.LuaTable table = tables.get("Difficulty");
        assertEquals("EASY", table.get(1).tojstring());
        assertEquals("NORMAL", table.get(2).tojstring());
        assertEquals("HARD", table.get(3).tojstring());
        // Named aliases for Lua-only enums are the value name strings
        assertEquals("EASY", table.get("EASY").tojstring());
        assertEquals("HARD", table.get("HARD").tojstring());
    }

    @Test
    public void testToLuaTablesNamedMembersUseJavaConstants() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum(RegistryTestEnum.class);

        org.luaj.vm2.LuaTable table = context.toLuaTables().get("RegistryTestEnum");
        assertNotNull(table);
        assertEquals("VALUE1", table.get(1).tojstring());
        assertTrue(table.get("VALUE1").isuserdata());
        assertSame(RegistryTestEnum.VALUE1, table.get("VALUE1").touserdata());
        assertSame(RegistryTestEnum.VALUE2, table.get("VALUE2").touserdata());
    }

    @Test
    public void testGetEnumNames() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("Enum1", Arrays.asList("A"));
        context.registerEnum("Enum2", Arrays.asList("B"));

        Set<String> names = context.getEnumNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("Enum1"));
        assertTrue(names.contains("Enum2"));
    }

    @Test
    public void testExtendEnumOnExisting() {
        EnumRegistry registry = new EnumRegistry();
        registry.registerEnum(RegistryTestEnum.class);

        // Extend with new values
        EnumDefinition extended = registry.extendEnum("RegistryTestEnum", Arrays.asList("VALUE4", "VALUE5"),
                Map.of("VALUE4", 10, "VALUE5", 20));

        assertNotNull(extended);
        assertEquals(5, extended.getValues().size());
        assertTrue(extended.hasValue("VALUE1"));
        assertTrue(extended.hasValue("VALUE2"));
        assertTrue(extended.hasValue("VALUE3"));
        assertTrue(extended.hasValue("VALUE4"));
        assertTrue(extended.hasValue("VALUE5"));
        assertEquals(10, extended.getValue("VALUE4").intValue());
        assertEquals(20, extended.getValue("VALUE5").intValue());
    }

    @Test
    public void testExtendEnumOnNonExistingReturnsNull() {
        EnumRegistry registry = new EnumRegistry();

        // Extend on non-existing enum should return null
        EnumDefinition result = registry.extendEnum("NewEnum", Arrays.asList("A", "B"),
                Map.of("A", 1, "B", 2));

        assertNull(result);
        assertFalse(registry.hasEnum("NewEnum"));
    }

    @Test
    public void testRegisterEnumRejectsDuplicates() {
        EnumRegistry registry = new EnumRegistry();

        registry.registerEnum(RegistryTestEnum.class);
        IllegalArgumentException classDuplicate = assertThrows(IllegalArgumentException.class,
                () -> registry.registerEnum(RegistryTestEnum.class));
        assertTrue(classDuplicate.getMessage().contains("already registered"));

        registry.registerEnum("CustomName", RegistryTestEnum.class);
        IllegalArgumentException nameDuplicate = assertThrows(IllegalArgumentException.class,
                () -> registry.registerEnum("CustomName", RegistryTestEnum.class));
        assertTrue(nameDuplicate.getMessage().contains("already registered"));

        registry.registerEnum("Test", Arrays.asList("A", "B"));
        IllegalArgumentException valuesDuplicate = assertThrows(IllegalArgumentException.class,
                () -> registry.registerEnum("Test", Arrays.asList("C", "D")));
        assertTrue(valuesDuplicate.getMessage().contains("already registered"));

        EnumDefinition original = registry.getEnum("Test");
        assertEquals(2, original.getValues().size());
        assertTrue(original.hasValue("A"));
        assertTrue(original.hasValue("B"));
        assertFalse(original.hasValue("C"));
    }
}

