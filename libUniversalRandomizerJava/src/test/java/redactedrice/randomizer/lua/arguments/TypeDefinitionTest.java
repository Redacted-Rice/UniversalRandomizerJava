package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TypeDefinitionTest {

    @Test
    public void testPrimitiveTypes() {
        assertEquals(ArgumentType.STRING, TypeDefinition.string().getBaseType());
        assertTrue(TypeDefinition.string().isPrimitive());

        assertEquals(ArgumentType.INTEGER, TypeDefinition.integer().getBaseType());
        assertTrue(TypeDefinition.integer().isPrimitive());

        assertEquals(ArgumentType.DOUBLE, TypeDefinition.doubleType().getBaseType());
        assertTrue(TypeDefinition.doubleType().isPrimitive());

        assertEquals(ArgumentType.BOOLEAN, TypeDefinition.bool().getBaseType());
        assertTrue(TypeDefinition.bool().isPrimitive());
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
    public void testInvalidFactoryArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.enumType(null));
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.enumType(""));
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.listOf(null));
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.tableOf(null,
                TypeDefinition.string()));
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.tableOf(
                TypeDefinition.string(), null));
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.tableOf(
                TypeDefinition.listOf(TypeDefinition.string()), TypeDefinition.integer()));
        assertThrows(IllegalArgumentException.class, () -> TypeDefinition.tableOf(
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()),
                TypeDefinition.integer()));
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
    public void testToString() {
        assertEquals("String", TypeDefinition.string().toString());
        assertEquals("Integer", TypeDefinition.integer().toString());
        assertEquals("Double", TypeDefinition.doubleType().toString());
        assertEquals("Boolean", TypeDefinition.bool().toString());
        assertEquals("Enum<TestEnum>", TypeDefinition.enumType("TestEnum").toString());
        assertEquals("List<String>", TypeDefinition.listOf(TypeDefinition.string()).toString());
        assertEquals("Table<String, Integer>",
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer())
                        .toString());
    }

    @Test
    public void testTypePredicates() {
        assertTrue(TypeDefinition.string().isPrimitive());
        assertTrue(TypeDefinition.integer().isPrimitive());
        assertTrue(TypeDefinition.doubleType().isPrimitive());
        assertTrue(TypeDefinition.bool().isPrimitive());
        assertFalse(TypeDefinition.enumType("Test").isPrimitive());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isPrimitive());
        assertFalse(TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer())
                .isPrimitive());

        assertTrue(TypeDefinition.enumType("Test").isEnum());
        assertFalse(TypeDefinition.string().isEnum());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isEnum());

        assertTrue(TypeDefinition.listOf(TypeDefinition.string()).isList());
        assertFalse(TypeDefinition.string().isList());
        assertFalse(
                TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()).isList());

        assertTrue(TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer()).isTable());
        assertFalse(TypeDefinition.string().isTable());
        assertFalse(TypeDefinition.listOf(TypeDefinition.string()).isTable());

        assertTrue(TypeDefinition.listOf(TypeDefinition.string()).isComplex());
        assertTrue(TypeDefinition.tableOf(TypeDefinition.string(), TypeDefinition.integer())
                .isComplex());
        assertFalse(TypeDefinition.string().isComplex());
        assertFalse(TypeDefinition.enumType("Test").isComplex());
    }

    @Test
    public void testEqualsAndHashCode() {
        TypeDefinition type1 = TypeDefinition.string();
        TypeDefinition type2 = TypeDefinition.string();
        TypeDefinition type3 = TypeDefinition.integer();

        assertEquals(type1, type2);
        assertNotEquals(type1, type3);
        assertNotEquals(type1, null);
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
        TypeDefinition type = TypeDefinition.parse("string");
        assertEquals(ArgumentType.STRING, type.getBaseType());
    }
}
