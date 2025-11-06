package redactedrice.randomizer.metadata;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TypeDefinitionTest {

    @Test
    public void testStringType() {
        TypeDefinition type = TypeDefinition.string();
        assertEquals(TypeDefinition.BaseType.STRING, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testIntegerType() {
        TypeDefinition type = TypeDefinition.integer();
        assertEquals(TypeDefinition.BaseType.INTEGER, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testDoubleType() {
        TypeDefinition type = TypeDefinition.doubleType();
        assertEquals(TypeDefinition.BaseType.DOUBLE, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testBooleanType() {
        TypeDefinition type = TypeDefinition.bool();
        assertEquals(TypeDefinition.BaseType.BOOLEAN, type.getBaseType());
        assertTrue(type.isPrimitive());
    }

    @Test
    public void testEnumType() {
        TypeDefinition type = TypeDefinition.enumType("EntityType");
        assertEquals(TypeDefinition.BaseType.ENUM, type.getBaseType());
        assertEquals("EntityType", type.getEnumName());
        assertTrue(type.isEnum());
    }

    @Test
    public void testListType() {
        TypeDefinition elementType = TypeDefinition.integer();
        TypeDefinition listType = TypeDefinition.listOf(elementType);
        assertEquals(TypeDefinition.BaseType.LIST, listType.getBaseType());
        assertEquals(elementType, listType.getElementType());
        assertTrue(listType.isList());
        assertTrue(listType.isComplex());
    }

    @Test
    public void testMapType() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.integer();
        TypeDefinition mapType = TypeDefinition.mapOf(keyType, valueType);
        assertEquals(TypeDefinition.BaseType.MAP, mapType.getBaseType());
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
        assertEquals(TypeDefinition.BaseType.GROUP, groupType.getBaseType());
        assertEquals(keyType, groupType.getKeyType());
        assertEquals(listValueType, groupType.getValueType());
    }

    @Test
    public void testParseSimpleType() {
        TypeDefinition stringType = TypeDefinition.parse("string");
        assertEquals(TypeDefinition.BaseType.STRING, stringType.getBaseType());

        TypeDefinition intType = TypeDefinition.parse("integer");
        assertEquals(TypeDefinition.BaseType.INTEGER, intType.getBaseType());

        TypeDefinition intType2 = TypeDefinition.parse("int");
        assertEquals(TypeDefinition.BaseType.INTEGER, intType2.getBaseType());
    }

    @Test
    public void testParseComplexType() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        enumSpec.put("enumName", "EntityType");
        TypeDefinition enumType = TypeDefinition.parse(enumSpec);
        assertEquals(TypeDefinition.BaseType.ENUM, enumType.getBaseType());
        assertEquals("EntityType", enumType.getEnumName());
    }

    @Test
    public void testParseListType() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", "integer");
        TypeDefinition listType = TypeDefinition.parse(listSpec);
        assertEquals(TypeDefinition.BaseType.LIST, listType.getBaseType());
        assertEquals(TypeDefinition.BaseType.INTEGER, listType.getElementType().getBaseType());
    }

    @Test
    public void testParseMapType() {
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("keyDefinition", "string");
        mapSpec.put("valueDefinition", "integer");
        TypeDefinition mapType = TypeDefinition.parse(mapSpec);
        assertEquals(TypeDefinition.BaseType.MAP, mapType.getBaseType());
        assertEquals(TypeDefinition.BaseType.STRING, mapType.getKeyType().getBaseType());
        assertEquals(TypeDefinition.BaseType.INTEGER, mapType.getValueType().getBaseType());
    }

    @Test
    public void testParseInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse("invalid");
        });
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
    public void testParseInvalidTypeSpec() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(123); // Not String or Map
        });
    }

    @Test
    public void testParseComplexTypeMissingTypeField() {
        Map<String, Object> spec = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(spec);
        });
    }

    @Test
    public void testParseEnumTypeWithConstraint() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        enumSpec.put("constraint", "EntityType");
        TypeDefinition enumType = TypeDefinition.parse(enumSpec);
        assertEquals(TypeDefinition.BaseType.ENUM, enumType.getBaseType());
    }

    @Test
    public void testParseEnumTypeWithEnumName() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        enumSpec.put("enumName", "EntityType");
        TypeDefinition enumType = TypeDefinition.parse(enumSpec);
        assertEquals("EntityType", enumType.getEnumName());
    }

    @Test
    public void testParseEnumTypeMissingNameThrows() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(enumSpec);
        });
    }

    @Test
    public void testParseListTypeMissingElementDefinition() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(listSpec);
        });
    }

    @Test
    public void testParseMapTypeMissingKeyDefinition() {
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(mapSpec);
        });
    }

    @Test
    public void testParseMapTypeMissingValueDefinition() {
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("keyDefinition", "string");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(mapSpec);
        });
    }

    @Test
    public void testParseGroupTypeMissingKeyDefinition() {
        Map<String, Object> groupSpec = new HashMap<>();
        groupSpec.put("type", "group");
        groupSpec.put("listElementDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(groupSpec);
        });
    }

    @Test
    public void testParseGroupTypeMissingListElementDefinition() {
        Map<String, Object> groupSpec = new HashMap<>();
        groupSpec.put("type", "group");
        groupSpec.put("keyDefinition", "string");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.parse(groupSpec);
        });
    }

    @Test
    public void testParseWithConstraint() {
        Map<String, Object> constraintMap = new HashMap<>();
        constraintMap.put("type", "range");
        constraintMap.put("min", 1.0);
        constraintMap.put("max", 100.0);

        Map<String, Object> typeSpec = new HashMap<>();
        typeSpec.put("type", "integer");
        typeSpec.put("constraint", constraintMap);

        TypeDefinition type = TypeDefinition.parse(typeSpec);
        assertNotNull(type.getConstraint());
    }

    @Test
    public void testParseWithDiscreteRangeConstraint() {
        Map<String, Object> constraintMap = new HashMap<>();
        constraintMap.put("type", "discrete_range");
        constraintMap.put("min", 0.0);
        constraintMap.put("max", 100.0);
        constraintMap.put("step", 10.0);

        Map<String, Object> typeSpec = new HashMap<>();
        typeSpec.put("type", "integer");
        typeSpec.put("constraint", constraintMap);

        TypeDefinition type = TypeDefinition.parse(typeSpec);
        assertNotNull(type.getConstraint());
    }

    @Test
    public void testParseWithEnumConstraint() {
        Map<String, Object> constraintMap = new HashMap<>();
        constraintMap.put("type", "enum");
        constraintMap.put("values", java.util.Arrays.asList("A", "B", "C"));

        Map<String, Object> typeSpec = new HashMap<>();
        typeSpec.put("type", "string");
        typeSpec.put("constraint", constraintMap);

        TypeDefinition type = TypeDefinition.parse(typeSpec);
        assertNotNull(type.getConstraint());
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
    public void testParseAllSimpleTypes() {
        assertEquals(TypeDefinition.BaseType.STRING, TypeDefinition.parse("string").getBaseType());
        assertEquals(TypeDefinition.BaseType.INTEGER,
                TypeDefinition.parse("integer").getBaseType());
        assertEquals(TypeDefinition.BaseType.INTEGER, TypeDefinition.parse("int").getBaseType());
        assertEquals(TypeDefinition.BaseType.DOUBLE, TypeDefinition.parse("double").getBaseType());
        assertEquals(TypeDefinition.BaseType.DOUBLE, TypeDefinition.parse("number").getBaseType());
        assertEquals(TypeDefinition.BaseType.BOOLEAN,
                TypeDefinition.parse("boolean").getBaseType());
        assertEquals(TypeDefinition.BaseType.BOOLEAN, TypeDefinition.parse("bool").getBaseType());
    }
}

