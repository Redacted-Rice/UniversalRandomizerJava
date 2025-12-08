package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TypeDefinitionTest {

    @Test
    public void testStringType() {
        TypeDefinition type = TypeDefinition.string();
        assertEquals(ArgumentType.STRING, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testIntegerType() {
        TypeDefinition type = TypeDefinition.integer();
        assertEquals(ArgumentType.INTEGER, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testDoubleType() {
        TypeDefinition type = TypeDefinition.doubleType();
        assertEquals(ArgumentType.DOUBLE, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testBooleanType() {
        TypeDefinition type = TypeDefinition.bool();
        assertEquals(ArgumentType.BOOLEAN, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testEnumType() {
        TypeDefinition type = TypeDefinition.enumType("EntityType");
        assertEquals(ArgumentType.ENUM, type.getBaseType());
        assertEquals("EntityType", type.getEnumName());
        assertTrue(type.isEnum());
    }

    @Test
    public void testListType() {
        TypeDefinition elementType = TypeDefinition.integer();
        TypeDefinition listType = TypeDefinition.listOf(elementType);
        assertEquals(ArgumentType.LIST, listType.getBaseType());
        assertEquals(elementType, listType.getElementType());
        assertTrue(listType.isList());
        assertTrue(listType.isComplex());
    }

    @Test
    public void testMapType() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.integer();
        TypeDefinition mapType = TypeDefinition.mapOf(keyType, valueType);
        assertEquals(ArgumentType.MAP, mapType.getBaseType());
        assertEquals(keyType, mapType.getKeyType());
        assertEquals(valueType, mapType.getValueType());
        assertTrue(mapType.isMap());
        assertTrue(mapType.isComplex());
    }

    @Test
    public void testGroupType() {
        TypeDefinition keyType = TypeDefinition.enumType("EntityType");
        TypeDefinition listValueType = TypeDefinition.listOf(TypeDefinition.integer());
        TypeDefinition groupType = TypeDefinition.groupOf(keyType, listValueType);
        assertEquals(ArgumentType.GROUP, groupType.getBaseType());
        assertEquals(keyType, groupType.getKeyType());
        assertEquals(listValueType, groupType.getValueType());
    }

    @Test
    public void testToString() {
        TypeDefinition stringType = TypeDefinition.string();
        assertEquals("String", stringType.toString());

        TypeDefinition enumType = TypeDefinition.enumType("EntityType");
        assertEquals("Enum<EntityType>", enumType.toString());

        TypeDefinition listType = TypeDefinition.listOf(TypeDefinition.integer());
        assertEquals("List<Integer>", listType.toString());
    }

    @Test
    public void testEnumTypeNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.enumType(null);
        });
    }

    @Test
    public void testEnumTypeEmptyNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.enumType("");
        });
    }

    @Test
    public void testListOfNullElementThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.listOf(null);
        });
    }

    @Test
    public void testMapOfNullKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.mapOf(null, TypeDefinition.string());
        });
    }

    @Test
    public void testMapOfNullValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.mapOf(TypeDefinition.string(), null);
        });
    }

    @Test
    public void testGroupOfNullKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.groupOf(null, TypeDefinition.listOf(TypeDefinition.string()));
        });
    }

    @Test
    public void testGroupOfNullValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.groupOf(TypeDefinition.string(), null);
        });
    }

    @Test
    public void testToStringForAllTypes() {
        assertEquals("String", TypeDefinition.string().toString());
        assertEquals("Integer", TypeDefinition.integer().toString());
        assertEquals("Double", TypeDefinition.doubleType().toString());
        assertEquals("Boolean", TypeDefinition.bool().toString());
        assertEquals("Enum<TestEnum>", TypeDefinition.enumType("TestEnum").toString());

        TypeDefinition listType = TypeDefinition.listOf(TypeDefinition.string());
        assertEquals("List<String>", listType.toString());

        TypeDefinition mapType =
                TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer());
        assertEquals("Map<String, Integer>", mapType.toString());

        TypeDefinition groupType = TypeDefinition.groupOf(TypeDefinition.string(),
                TypeDefinition.listOf(TypeDefinition.integer()));
        assertEquals("Group<String, List<Integer>>", groupType.toString());
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(TypeDefinition.string().isPrimitive());
        assertTrue(TypeDefinition.integer().isPrimitive());
        assertTrue(TypeDefinition.doubleType().isPrimitive());
        assertTrue(TypeDefinition.bool().isPrimitive());
        assertFalse(TypeDefinition.enumType("Test").isPrimitive());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isPrimitive());
        assertFalse(TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer())
                .isPrimitive());
    }

    @Test
    public void testIsEnum() {
        assertTrue(TypeDefinition.enumType("Test").isEnum());
        assertFalse(TypeDefinition.string().isEnum());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isEnum());
    }

    @Test
    public void testIsList() {
        assertTrue(TypeDefinition.listOf(TypeDefinition.string()).isList());
        assertFalse(TypeDefinition.string().isList());
        assertFalse(
                TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer()).isList());
    }

    @Test
    public void testIsMap() {
        assertTrue(TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer()).isMap());
        assertFalse(TypeDefinition.string().isMap());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isMap());
    }

    @Test
    public void testIsComplex() {
        assertTrue(TypeDefinition.listOf(TypeDefinition.string()).isComplex());
        assertTrue(TypeDefinition.mapOf(TypeDefinition.string(), TypeDefinition.integer())
                .isComplex());
        assertFalse(TypeDefinition.string().isComplex());
        assertFalse(TypeDefinition.enumType("Test").isComplex());
    }

    @Test
    public void testEquals() {
        TypeDefinition type1 = TypeDefinition.string();
        TypeDefinition type2 = TypeDefinition.string();
        TypeDefinition type3 = TypeDefinition.integer();

        assertEquals(type1, type2);
        assertNotEquals(type1, type3);
        assertNotEquals(type1, null);
    }

    @Test
    public void testHashCode() {
        TypeDefinition type1 = TypeDefinition.string();
        TypeDefinition type2 = TypeDefinition.string();
        assertEquals(type1.hashCode(), type2.hashCode());
    }

    @Test
    public void testWithConstraint() {
        ArgumentConstraint constraint = ArgumentConstraint.range(1, 100);
        TypeDefinition type = TypeDefinition.integer(constraint);
        assertEquals(constraint, type.getConstraint());
    }

    @Test
    public void testParseDelegation() {
        // Verify TypeDefinition.parse() properly delegates to TypeParser
        TypeDefinition type = TypeDefinition.parse("string");
        assertEquals(ArgumentType.STRING, type.getBaseType());
    }
}

