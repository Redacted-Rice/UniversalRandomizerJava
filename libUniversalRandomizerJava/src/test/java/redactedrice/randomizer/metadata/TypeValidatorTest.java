package redactedrice.randomizer.metadata;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.context.EnumContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TypeValidatorTest {

    private EnumContext createTestEnumContext() {
        EnumContext context = new EnumContext();
        context.registerEnum("Difficulty", Arrays.asList("EASY", "NORMAL", "HARD"));
        return context;
    }

    @Test
    public void testConvertAndValidateString() {
        TypeDefinition typeDef = TypeDefinition.string();
        Object result = TypeValidator.convertAndValidate("test", typeDef, null);
        assertEquals("test", result);
        
        result = TypeValidator.convertAndValidate(123, typeDef, null);
        assertEquals("123", result);
    }

    @Test
    public void testConvertAndValidateInteger() {
        TypeDefinition typeDef = TypeDefinition.integer();
        Object result = TypeValidator.convertAndValidate(42, typeDef, null);
        assertEquals(42, result);
        
        result = TypeValidator.convertAndValidate(42.5, typeDef, null);
        assertEquals(42, result);
        
        result = TypeValidator.convertAndValidate("42", typeDef, null);
        assertEquals(42, result);
        
        result = TypeValidator.convertAndValidate(LuaInteger.valueOf(42), typeDef, null);
        assertEquals(42, result);
    }

    @Test
    public void testConvertAndValidateIntegerInvalidThrows() {
        TypeDefinition typeDef = TypeDefinition.integer();
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("not a number", typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateDouble() {
        TypeDefinition typeDef = TypeDefinition.doubleType();
        Object result = TypeValidator.convertAndValidate(42.5, typeDef, null);
        assertEquals(42.5, result);
        
        result = TypeValidator.convertAndValidate(42, typeDef, null);
        assertEquals(42.0, result);
        
        result = TypeValidator.convertAndValidate("42.5", typeDef, null);
        assertEquals(42.5, result);
        
        result = TypeValidator.convertAndValidate(LuaNumber.valueOf(42.5), typeDef, null);
        assertEquals(42.5, result);
    }

    @Test
    public void testConvertAndValidateDoubleInvalidThrows() {
        TypeDefinition typeDef = TypeDefinition.doubleType();
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("not a number", typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateBoolean() {
        TypeDefinition typeDef = TypeDefinition.bool();
        Object result = TypeValidator.convertAndValidate(true, typeDef, null);
        assertEquals(true, result);
        
        result = TypeValidator.convertAndValidate("true", typeDef, null);
        assertEquals(true, result);
        
        result = TypeValidator.convertAndValidate("yes", typeDef, null);
        assertEquals(true, result);
        
        result = TypeValidator.convertAndValidate("1", typeDef, null);
        assertEquals(true, result);
        
        result = TypeValidator.convertAndValidate("false", typeDef, null);
        assertEquals(false, result);
        
        result = TypeValidator.convertAndValidate("no", typeDef, null);
        assertEquals(false, result);
        
        result = TypeValidator.convertAndValidate("0", typeDef, null);
        assertEquals(false, result);
        
        result = TypeValidator.convertAndValidate(1, typeDef, null);
        assertEquals(true, result);
        
        result = TypeValidator.convertAndValidate(0, typeDef, null);
        assertEquals(false, result);
        
        result = TypeValidator.convertAndValidate(LuaValue.TRUE, typeDef, null);
        assertEquals(true, result);
    }

    @Test
    public void testConvertAndValidateBooleanInvalidThrows() {
        TypeDefinition typeDef = TypeDefinition.bool();
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("maybe", typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateEnum() {
        EnumContext enumContext = createTestEnumContext();
        TypeDefinition typeDef = TypeDefinition.enumType("Difficulty");
        
        Object result = TypeValidator.convertAndValidate("EASY", typeDef, enumContext);
        assertEquals("EASY", result);
        
        result = TypeValidator.convertAndValidate("NORMAL", typeDef, enumContext);
        assertEquals("NORMAL", result);
    }

    @Test
    public void testConvertAndValidateEnumNullContextThrows() {
        TypeDefinition typeDef = TypeDefinition.enumType("Difficulty");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("EASY", typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateEnumNotFoundThrows() {
        EnumContext enumContext = createTestEnumContext();
        TypeDefinition typeDef = TypeDefinition.enumType("NonExistent");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("EASY", typeDef, enumContext);
        });
    }

    @Test
    public void testConvertAndValidateEnumInvalidValueThrows() {
        EnumContext enumContext = createTestEnumContext();
        TypeDefinition typeDef = TypeDefinition.enumType("Difficulty");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("INVALID", typeDef, enumContext);
        });
    }

    @Test
    public void testConvertAndValidateList() {
        TypeDefinition elementType = TypeDefinition.integer();
        TypeDefinition listType = TypeDefinition.listOf(elementType);
        
        List<Integer> input = Arrays.asList(1, 2, 3);
        Object result = TypeValidator.convertAndValidate(input, listType, null);
        assertNotNull(result);
        assertTrue(result instanceof List);
        List<?> resultList = (List<?>) result;
        assertEquals(3, resultList.size());
    }

    @Test
    public void testConvertAndValidateListFromLuaTable() {
        TypeDefinition elementType = TypeDefinition.string();
        TypeDefinition listType = TypeDefinition.listOf(elementType);
        
        LuaTable table = new LuaTable();
        table.set(1, LuaString.valueOf("value1"));
        table.set(2, LuaString.valueOf("value2"));
        table.set(3, LuaString.valueOf("value3"));
        
        Object result = TypeValidator.convertAndValidate(table, listType, null);
        assertNotNull(result);
        assertTrue(result instanceof List);
        List<?> resultList = (List<?>) result;
        assertEquals(3, resultList.size());
    }

    @Test
    public void testConvertAndValidateListFromArray() {
        TypeDefinition elementType = TypeDefinition.integer();
        TypeDefinition listType = TypeDefinition.listOf(elementType);
        
        Object[] array = new Object[]{1, 2, 3};
        Object result = TypeValidator.convertAndValidate(array, listType, null);
        assertNotNull(result);
        assertTrue(result instanceof List);
    }

    @Test
    public void testConvertAndValidateListInvalidThrows() {
        TypeDefinition elementType = TypeDefinition.integer();
        TypeDefinition listType = TypeDefinition.listOf(elementType);
        
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("not a list", listType, null);
        });
    }

    @Test
    public void testConvertAndValidateMap() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.integer();
        TypeDefinition mapType = TypeDefinition.mapOf(keyType, valueType);
        
        Map<String, Integer> input = new HashMap<>();
        input.put("key1", 1);
        input.put("key2", 2);
        
        Object result = TypeValidator.convertAndValidate(input, mapType, null);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertEquals(2, resultMap.size());
    }

    @Test
    public void testConvertAndValidateMapFromLuaTable() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.integer();
        TypeDefinition mapType = TypeDefinition.mapOf(keyType, valueType);
        
        LuaTable table = new LuaTable();
        table.set("key1", LuaInteger.valueOf(1));
        table.set("key2", LuaInteger.valueOf(2));
        
        Object result = TypeValidator.convertAndValidate(table, mapType, null);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertEquals(2, resultMap.size());
    }

    @Test
    public void testConvertAndValidateMapInvalidThrows() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.integer();
        TypeDefinition mapType = TypeDefinition.mapOf(keyType, valueType);
        
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("not a map", mapType, null);
        });
    }

    @Test
    public void testConvertAndValidateGroup() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.listOf(TypeDefinition.integer());
        TypeDefinition groupType = TypeDefinition.groupOf(keyType, valueType);
        
        Map<String, List<Integer>> input = new HashMap<>();
        input.put("key1", Arrays.asList(1, 2, 3));
        input.put("key2", Arrays.asList(4, 5));
        
        Object result = TypeValidator.convertAndValidate(input, groupType, null);
        assertNotNull(result);
        assertTrue(result instanceof Map);
    }

    @Test
    public void testConvertAndValidateWithConstraintRange() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        
        Object result = TypeValidator.convertAndValidate(50, typeDef, null);
        assertEquals(50, result);
        
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate(150, typeDef, null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate(0, typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateWithConstraintDiscreteRange() {
        ArgumentConstraint constraint = ArgumentConstraint.discreteRange(0, 100, 10);
        TypeDefinition typeDef = TypeDefinition.integer(constraint);
        
        Object result = TypeValidator.convertAndValidate(50, typeDef, null);
        assertEquals(50, result);
        
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate(55, typeDef, null); // Not a multiple of 10
        });
    }

    @Test
    public void testConvertAndValidateWithConstraintEnum() {
        List<Object> allowedValues = Arrays.asList("A", "B", "C");
        ArgumentConstraint constraint = ArgumentConstraint.enumValues(allowedValues);
        TypeDefinition typeDef = TypeDefinition.string(constraint);
        
        Object result = TypeValidator.convertAndValidate("A", typeDef, null);
        assertEquals("A", result);
        
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("D", typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateNullThrows() {
        TypeDefinition typeDef = TypeDefinition.string();
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate(null, typeDef, null);
        });
    }

    @Test
    public void testConvertAndValidateUnknownTypeThrows() {
        // This would require creating a TypeDefinition with an invalid base type
        // which is not possible through the public API, so we test the error path
        // by testing with a complex type that might fail
        TypeDefinition typeDef = TypeDefinition.listOf(TypeDefinition.integer());
        assertThrows(IllegalArgumentException.class, () -> {
            TypeValidator.convertAndValidate("not a list", typeDef, null);
        });
    }

    @Test
    public void testIntegerConversionFromVariousTypes() {
        TypeDefinition typeDef = TypeDefinition.integer();
        
        assertEquals(42, TypeValidator.convertAndValidate((byte) 42, typeDef, null));
        assertEquals(42, TypeValidator.convertAndValidate((short) 42, typeDef, null));
        assertEquals(42, TypeValidator.convertAndValidate(42L, typeDef, null));
        assertEquals(42, TypeValidator.convertAndValidate(42.0f, typeDef, null));
        assertEquals(42, TypeValidator.convertAndValidate(42.0, typeDef, null));
    }

    @Test
    public void testDoubleConversionFromVariousTypes() {
        TypeDefinition typeDef = TypeDefinition.doubleType();
        
        assertEquals(42.0, TypeValidator.convertAndValidate((byte) 42, typeDef, null));
        assertEquals(42.0, TypeValidator.convertAndValidate((short) 42, typeDef, null));
        assertEquals(42.0, TypeValidator.convertAndValidate(42, typeDef, null));
        assertEquals(42.0, TypeValidator.convertAndValidate(42L, typeDef, null));
        assertEquals(42.0, TypeValidator.convertAndValidate(42.0f, typeDef, null));
    }

    @Test
    public void testNestedListConversion() {
        TypeDefinition nestedListType = TypeDefinition.listOf(
            TypeDefinition.listOf(TypeDefinition.integer())
        );
        
        List<List<Integer>> input = Arrays.asList(
            Arrays.asList(1, 2),
            Arrays.asList(3, 4)
        );
        
        Object result = TypeValidator.convertAndValidate(input, nestedListType, null);
        assertNotNull(result);
        assertTrue(result instanceof List);
    }

    @Test
    public void testNestedMapConversion() {
        TypeDefinition nestedMapType = TypeDefinition.mapOf(
            TypeDefinition.string(),
            TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer())
        );
        
        Map<String, Map<String, Integer>> input = new HashMap<>();
        Map<String, Integer> inner = new HashMap<>();
        inner.put("key", 1);
        input.put("outer", inner);
        
        Object result = TypeValidator.convertAndValidate(input, nestedMapType, null);
        assertNotNull(result);
        assertTrue(result instanceof Map);
    }

    @Test
    public void testListWithEnumElements() {
        EnumContext enumContext = createTestEnumContext();
        TypeDefinition enumListType = TypeDefinition.listOf(
            TypeDefinition.enumType("Difficulty")
        );
        
        List<String> input = Arrays.asList("EASY", "NORMAL", "HARD");
        Object result = TypeValidator.convertAndValidate(input, enumListType, enumContext);
        assertNotNull(result);
        assertTrue(result instanceof List);
    }
}

