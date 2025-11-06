package redactedrice.randomizer.context;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EnumContextTest {

    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    enum TestEnumWithValue implements EnumValueProvider {
        LOW(1), MEDIUM(10), HIGH(100);

        private final int value;

        TestEnumWithValue(int value) {
            this.value = value;
        }

        @Override
        public int getIntValue() {
            return value;
        }
    }

    @Test
    public void testRegisterEnumFromClass() {
        EnumContext context = new EnumContext();
        context.registerEnum(TestEnum.class);

        assertTrue(context.hasEnum("TestEnum"));
        EnumDefinition def = context.getEnum("TestEnum");
        assertNotNull(def);
        assertTrue(def.hasValue("VALUE1"));
        assertTrue(def.hasValue("VALUE2"));
        assertTrue(def.hasValue("VALUE3"));
    }

    @Test
    public void testRegisterEnumWithCustomName() {
        EnumContext context = new EnumContext();
        context.registerEnum("CustomName", TestEnum.class);

        assertTrue(context.hasEnum("CustomName"));
        assertFalse(context.hasEnum("TestEnum"));
    }

    @Test
    public void testRegisterEnumWithValues() {
        EnumContext context = new EnumContext();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"), null);

        assertTrue(context.hasEnum("Difficulty"));
        EnumDefinition def = context.getEnum("Difficulty");
        assertTrue(def.hasValue("EASY"));
        assertTrue(def.hasValue("NORMAL"));
        assertTrue(def.hasValue("HARD"));
    }

    @Test
    public void testRegisterEnumWithValueMap() {
        EnumContext context = new EnumContext();
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
        EnumContext context = new EnumContext();
        context.registerEnum("Priority", TestEnumWithValue.class);

        EnumDefinition def = context.getEnum("Priority");
        assertEquals(1, def.getValue("LOW").intValue());
        assertEquals(10, def.getValue("MEDIUM").intValue());
        assertEquals(100, def.getValue("HIGH").intValue());
    }

    @Test
    public void testStringToEnum() {
        EnumContext context = new EnumContext();
        context.registerEnum("TestEnum", TestEnum.class);

        Object result = context.stringToEnum("TestEnum", "VALUE1");
        assertEquals(TestEnum.VALUE1, result);

        Object result2 = context.stringToEnum("TestEnum", "INVALID");
        assertNull(result2);
    }

    @Test
    public void testIsValidEnumValue() {
        EnumContext context = new EnumContext();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        assertTrue(context.isValidEnumValue("Difficulty", "EASY"));
        assertTrue(context.isValidEnumValue("Difficulty", "NORMAL"));
        assertFalse(context.isValidEnumValue("Difficulty", "INVALID"));
    }

    @Test
    public void testMergeFrom() {
        EnumContext source = new EnumContext();
        source.registerEnum("Enum1", Arrays.asList("A", "B"));

        EnumContext target = new EnumContext();
        target.registerEnum("Enum2", Arrays.asList("C", "D"));

        target.mergeFrom(source);
        assertTrue(target.hasEnum("Enum1"));
        assertTrue(target.hasEnum("Enum2"));
    }

    @Test
    public void testToLuaTables() {
        EnumContext context = new EnumContext();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));

        Map<String, org.luaj.vm2.LuaTable> tables = context.toLuaTables();
        assertTrue(tables.containsKey("Difficulty"));

        org.luaj.vm2.LuaTable table = tables.get("Difficulty");
        assertEquals("EASY", table.get(1).tojstring());
        assertEquals("NORMAL", table.get(2).tojstring());
        assertEquals("HARD", table.get(3).tojstring());
    }

    @Test
    public void testGetEnumNames() {
        EnumContext context = new EnumContext();
        context.registerEnum("Enum1", Arrays.asList("A"));
        context.registerEnum("Enum2", Arrays.asList("B"));

        Set<String> names = context.getEnumNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("Enum1"));
        assertTrue(names.contains("Enum2"));
    }
}

