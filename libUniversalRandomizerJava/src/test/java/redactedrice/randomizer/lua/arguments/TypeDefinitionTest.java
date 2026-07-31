package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

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
    public void testTableType() {
        TypeDefinition keyType = TypeDefinition.string();
        TypeDefinition valueType = TypeDefinition.integer();
        TypeDefinition tableType = TypeDefinition.tableOf(keyType, valueType);
        assertEquals(ArgumentType.TABLE, tableType.getBaseType());
        assertEquals(keyType, tableType.getKeyType());
        assertEquals(valueType, tableType.getValueType());
        assertTrue(tableType.isTable());
        assertTrue(tableType.isComplex());
    }

    @Test
    public void testTableWithListValueType() {
        TypeDefinition keyType = TypeDefinition.enumType("EntityType");
        TypeDefinition listValueType = TypeDefinition.listOf(TypeDefinition.integer());
        TypeDefinition tableType = TypeDefinition.tableOf(keyType, listValueType);
        assertEquals(ArgumentType.TABLE, tableType.getBaseType());
        assertEquals(keyType, tableType.getKeyType());
        assertEquals(listValueType, tableType.getValueType());
        assertTrue(tableType.getValueType().isList());
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
    public void testTableOfNullKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.tableOf(null, TypeDefinition.string());
        });
    }

    @Test
    public void testTableOfNullValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.tableOf(TypeDefinition.string(), null);
        });
    }

    @Test
    public void testTableOfListKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.tableOf(TypeDefinition.listOf(TypeDefinition.string()),
                    TypeDefinition.integer());
        });
    }

    @Test
    public void testTableOfTableKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeDefinition.tableOf(
                    TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()),
                    TypeDefinition.integer());
        });
    }

    @Test
    public void testTableValueMayBeListOrTable() {
        TypeDefinition withListValue = TypeDefinition.tableOf(TypeDefinition.string(),
                TypeDefinition.listOf(TypeDefinition.integer()));
        assertTrue(withListValue.getValueType().isList());

        TypeDefinition withNestedTableValue = TypeDefinition.tableOf(TypeDefinition.string(),
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()));
        assertTrue(withNestedTableValue.getValueType().isTable());
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

        TypeDefinition tableType =
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer());
        assertEquals("Table<String, Integer>", tableType.toString());
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(TypeDefinition.string().isPrimitive());
        assertTrue(TypeDefinition.integer().isPrimitive());
        assertTrue(TypeDefinition.doubleType().isPrimitive());
        assertTrue(TypeDefinition.bool().isPrimitive());
        assertFalse(TypeDefinition.enumType("Test").isPrimitive());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isPrimitive());
        assertFalse(TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer())
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
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()).isList());
    }

    @Test
    public void testIsTable() {
        assertTrue(TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()).isTable());
        assertFalse(TypeDefinition.string().isTable());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isTable());
    }

    @Test
    public void testIsComplex() {
        assertTrue(TypeDefinition.listOf(TypeDefinition.string()).isComplex());
        assertTrue(TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer())
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

