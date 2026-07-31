package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.context.EnumRegistry;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentConverterTest {

    private EnumRegistry createTestEnumRegistry() {
        EnumRegistry context = new EnumRegistry();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));
        return context;
    }

    @Test
    public void testConvertAndValidateString() {
        TypeDefinition typeDef = TypeDefinition.string();
        assertEquals("test", ArgumentConverter.convertAndValidate("test", typeDef, null));
        assertEquals("123", ArgumentConverter.convertAndValidate(123, typeDef, null));
    }

    @Test
    public void testConvertAndValidateInteger() {
        TypeDefinition typeDef = TypeDefinition.integer();
        assertEquals(42, ArgumentConverter.convertAndValidate(42, typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate(42.5, typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate("42", typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate(LuaInteger.valueOf(42), typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate((byte) 42, typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate((short) 42, typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate(42L, typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate(42.0f, typeDef, null));
        assertEquals(42, ArgumentConverter.convertAndValidate(42.0, typeDef, null));

        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("not a number", typeDef, null));
    }

    @Test
    public void testConvertAndValidateDouble() {
        TypeDefinition typeDef = TypeDefinition.doubleType();
        assertEquals(42.5, ArgumentConverter.convertAndValidate(42.5, typeDef, null));
        assertEquals(42.0, ArgumentConverter.convertAndValidate(42, typeDef, null));
        assertEquals(42.5, ArgumentConverter.convertAndValidate("42.5", typeDef, null));
        assertEquals(42.5, ArgumentConverter.convertAndValidate(LuaNumber.valueOf(42.5), typeDef, null));
        assertEquals(42.0, ArgumentConverter.convertAndValidate((byte) 42, typeDef, null));
        assertEquals(42.0, ArgumentConverter.convertAndValidate((short) 42, typeDef, null));
        assertEquals(42.0, ArgumentConverter.convertAndValidate(42L, typeDef, null));
        assertEquals(42.0, ArgumentConverter.convertAndValidate(42.0f, typeDef, null));

        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("not a number", typeDef, null));
    }

    @Test
    public void testConvertAndValidateBoolean() {
        TypeDefinition typeDef = TypeDefinition.bool();
        assertEquals(true, ArgumentConverter.convertAndValidate(true, typeDef, null));
        assertEquals(true, ArgumentConverter.convertAndValidate("true", typeDef, null));
        assertEquals(true, ArgumentConverter.convertAndValidate("yes", typeDef, null));
        assertEquals(true, ArgumentConverter.convertAndValidate("1", typeDef, null));
        assertEquals(false, ArgumentConverter.convertAndValidate("false", typeDef, null));
        assertEquals(false, ArgumentConverter.convertAndValidate("no", typeDef, null));
        assertEquals(false, ArgumentConverter.convertAndValidate("0", typeDef, null));
        assertEquals(true, ArgumentConverter.convertAndValidate(1, typeDef, null));
        assertEquals(false, ArgumentConverter.convertAndValidate(0, typeDef, null));
        assertEquals(true, ArgumentConverter.convertAndValidate(LuaValue.TRUE, typeDef, null));

        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("maybe", typeDef, null));
    }

    @Test
    public void testConvertAndValidateEnum() {
        EnumRegistry enumContext = createTestEnumRegistry();
        TypeDefinition typeDef = TypeDefinition.enumType("Difficulty");

        assertEquals("EASY", ArgumentConverter.convertAndValidate("EASY", typeDef, enumContext));
        assertEquals("NORMAL", ArgumentConverter.convertAndValidate("NORMAL", typeDef, enumContext));

        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("EASY", typeDef, null));
        assertThrows(IllegalArgumentException.class, () -> ArgumentConverter
                .convertAndValidate("EASY", TypeDefinition.enumType("NonExistent"), enumContext));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("INVALID", typeDef, enumContext));
    }

    @Test
    public void testConvertAndValidateListFromMultipleSources() {
        TypeDefinition intList = TypeDefinition.listOf(TypeDefinition.integer());
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<?> fromList = (List<?>) ArgumentConverter.convertAndValidate(input, intList, null);
        assertEquals(3, fromList.size());

        LuaTable table = new LuaTable();
        table.set(1, LuaInteger.valueOf(1));
        table.set(2, LuaInteger.valueOf(2));
        table.set(3, LuaInteger.valueOf(3));
        List<?> fromTable = (List<?>) ArgumentConverter.convertAndValidate(table, intList, null);
        assertEquals(3, fromTable.size());

        Object[] array = new Object[] {1, 2, 3};
        assertTrue(ArgumentConverter.convertAndValidate(array, intList, null) instanceof List);

        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("not a list", intList, null));
    }

    @Test
    public void testConvertAndValidateTableFromMultipleSources() {
        TypeDefinition tableType =
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer());

        Map<String, Integer> input = new HashMap<>();
        input.put("key1", 1);
        input.put("key2", 2);
        Map<?, ?> fromMap =
                (Map<?, ?>) ArgumentConverter.convertAndValidate(input, tableType, null);
        assertEquals(2, fromMap.size());

        LuaTable table = new LuaTable();
        table.set("key1", LuaInteger.valueOf(1));
        table.set("key2", LuaInteger.valueOf(2));
        Map<?, ?> fromLua =
                (Map<?, ?>) ArgumentConverter.convertAndValidate(table, tableType, null);
        assertEquals(2, fromLua.size());

        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("not a table", tableType, null));
    }

    @Test
    public void testConvertAndValidateTableWithListValues() {
        TypeDefinition tableType = TypeDefinition.tableOf(TypeDefinition.string(),
                TypeDefinition.listOf(TypeDefinition.integer()));

        Map<String, List<Integer>> input = new HashMap<>();
        input.put("key1", Arrays.asList(1, 2, 3));
        input.put("key2", Arrays.asList(4, 5));

        assertTrue(ArgumentConverter.convertAndValidate(input, tableType, null) instanceof Map);
    }

    @Test
    public void testConvertAndValidateWithConstraints() {
        TypeDefinition rangeType =
                TypeDefinition.integer(ArgumentConstraint.range(1, 100));
        assertEquals(50, ArgumentConverter.convertAndValidate(50, rangeType, null));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate(150, rangeType, null));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate(0, rangeType, null));

        TypeDefinition stepType =
                TypeDefinition.integer(ArgumentConstraint.discreteRange(0, 100, 10));
        assertEquals(50, ArgumentConverter.convertAndValidate(50, stepType, null));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate(55, stepType, null));

        TypeDefinition stringEnumType = TypeDefinition.string(
                ArgumentConstraint.enumValues(Arrays.asList("A", "B", "C")));
        assertEquals("A", ArgumentConverter.convertAndValidate("A", stringEnumType, null));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate("D", stringEnumType, null));

        TypeDefinition numericEnumType =
                TypeDefinition.integer(ArgumentConstraint.enumValues(Arrays.asList(1, 2, 3)));
        assertEquals(2, ArgumentConverter.convertAndValidate(2, numericEnumType, null));
        assertEquals(2, ArgumentConverter.convertAndValidate(2.0, numericEnumType, null));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate(4, numericEnumType, null));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate(4.0, numericEnumType, null));
    }

    @Test
    public void testConvertAndValidateNullThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentConverter.convertAndValidate(null, TypeDefinition.string(), null));
    }

    @Test
    public void testNestedConversions() {
        TypeDefinition nestedListType =
                TypeDefinition.listOf(TypeDefinition.listOf(TypeDefinition.integer()));
        List<List<Integer>> nestedList = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        assertTrue(
                ArgumentConverter.convertAndValidate(nestedList, nestedListType, null) instanceof List);

        TypeDefinition nestedTableType = TypeDefinition.tableOf(TypeDefinition.string(),
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()));
        Map<String, Map<String, Integer>> nestedTable = new HashMap<>();
        Map<String, Integer> inner = new HashMap<>();
        inner.put("key", 1);
        nestedTable.put("outer", inner);
        assertTrue(ArgumentConverter.convertAndValidate(nestedTable, nestedTableType,
                null) instanceof Map);

        TypeDefinition nestedGroupType = TypeDefinition.tableOf(TypeDefinition.string(),
                TypeDefinition.tableOf(TypeDefinition.string(),
                        TypeDefinition.listOf(TypeDefinition.integer())));
        Map<String, List<Integer>> firePools = Map.of("common", Arrays.asList(10, 20, 30), "rare",
                List.of(100));
        Map<String, List<Integer>> waterPools = Map.of("common", Arrays.asList(5, 15));
        Map<String, Map<String, List<Integer>>> grouped = Map.of("fire", firePools, "water",
                waterPools);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, List<Integer>>> result =
                (Map<String, Map<String, List<Integer>>>) ArgumentConverter
                        .convertAndValidate(grouped, nestedGroupType, null);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(10, 20, 30), result.get("fire").get("common"));
        assertEquals(Arrays.asList(100), result.get("fire").get("rare"));
        assertEquals(Arrays.asList(5, 15), result.get("water").get("common"));
    }

    @Test
    public void testListWithEnumElements() {
        EnumRegistry enumContext = createTestEnumRegistry();
        TypeDefinition enumListType = TypeDefinition.listOf(TypeDefinition.enumType("Difficulty"));
        List<String> input = Arrays.asList("EASY", "NORMAL", "HARD");
        assertTrue(ArgumentConverter.convertAndValidate(input, enumListType, enumContext) instanceof List);
    }
}
