package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TypeParserTest {

    @Test
    public void testParseSimpleType() {
        TypeDefinition stringType = TypeParser.parse("string");
        assertEquals(ArgumentType.STRING, stringType.getBaseType());

        TypeDefinition intType = TypeParser.parse("integer");
        assertEquals(ArgumentType.INTEGER, intType.getBaseType());

        TypeDefinition intType2 = TypeParser.parse("int");
        assertEquals(ArgumentType.INTEGER, intType2.getBaseType());
    }

    @Test
    public void testParseComplexType() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        enumSpec.put("enumName", "EntityType");
        TypeDefinition enumType = TypeParser.parse(enumSpec);
        assertEquals(ArgumentType.ENUM, enumType.getBaseType());
        assertEquals("EntityType", enumType.getEnumName());
    }

    @Test
    public void testParseListType() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", "integer");
        TypeDefinition listType = TypeParser.parse(listSpec);
        assertEquals(ArgumentType.LIST, listType.getBaseType());
        assertEquals(ArgumentType.INTEGER, listType.getElementType().getBaseType());
    }

    @Test
    public void testParseTableType() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        tableSpec.put("valueDefinition", "integer");
        TypeDefinition tableType = TypeParser.parse(tableSpec);
        assertEquals(ArgumentType.TABLE, tableType.getBaseType());
        assertEquals(ArgumentType.STRING, tableType.getKeyType().getBaseType());
        assertEquals(ArgumentType.INTEGER, tableType.getValueType().getBaseType());
    }

    @Test
    public void testParseInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse("invalid");
        });
    }

    @Test
    public void testParseInvalidTypeSpec() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(123); // Not String or Map
        });
    }

    @Test
    public void testParseComplexTypeMissingTypeField() {
        Map<String, Object> spec = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(spec);
        });
    }

    @Test
    public void testParseEnumTypeWithConstraint() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        enumSpec.put("constraint", "EntityType");
        TypeDefinition enumType = TypeParser.parse(enumSpec);
        assertEquals(ArgumentType.ENUM, enumType.getBaseType());
    }

    @Test
    public void testParseEnumTypeWithEnumName() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        enumSpec.put("enumName", "EntityType");
        TypeDefinition enumType = TypeParser.parse(enumSpec);
        assertEquals("EntityType", enumType.getEnumName());
    }

    @Test
    public void testParseEnumTypeMissingNameThrows() {
        Map<String, Object> enumSpec = new HashMap<>();
        enumSpec.put("type", "enum");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(enumSpec);
        });
    }

    @Test
    public void testParseListTypeMissingElementDefinition() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(listSpec);
        });
    }

    @Test
    public void testParseTableTypeMissingKeyDefinition() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(tableSpec);
        });
    }

    @Test
    public void testParseTableTypeMissingValueDefinition() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(tableSpec);
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

        TypeDefinition type = TypeParser.parse(typeSpec);
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

        TypeDefinition type = TypeParser.parse(typeSpec);
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

        TypeDefinition type = TypeParser.parse(typeSpec);
        assertNotNull(type.getConstraint());
    }

    @Test
    public void testParseAllSimpleTypes() {
        assertEquals(ArgumentType.STRING, TypeParser.parse("string").getBaseType());
        assertEquals(ArgumentType.INTEGER, TypeParser.parse("integer").getBaseType());
        assertEquals(ArgumentType.INTEGER, TypeParser.parse("int").getBaseType());
        assertEquals(ArgumentType.DOUBLE, TypeParser.parse("double").getBaseType());
        assertEquals(ArgumentType.DOUBLE, TypeParser.parse("number").getBaseType());
        assertEquals(ArgumentType.BOOLEAN, TypeParser.parse("boolean").getBaseType());
        assertEquals(ArgumentType.BOOLEAN, TypeParser.parse("bool").getBaseType());
    }

    @Test
    public void testParseTableWithListValueType() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", "integer");

        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        tableSpec.put("valueDefinition", listSpec);

        TypeDefinition tableType = TypeParser.parse(tableSpec);
        assertEquals(ArgumentType.TABLE, tableType.getBaseType());
        assertEquals(ArgumentType.STRING, tableType.getKeyType().getBaseType());
        assertEquals(ArgumentType.LIST, tableType.getValueType().getBaseType());
        assertEquals(ArgumentType.INTEGER,
                tableType.getValueType().getElementType().getBaseType());
    }

    @Test
    public void testParseNestedComplexTypes() {
        // List of tables: List<Table<String, Integer>>
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        tableSpec.put("valueDefinition", "integer");

        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", tableSpec);

        TypeDefinition listOfTablesType = TypeParser.parse(listSpec);
        assertEquals(ArgumentType.LIST, listOfTablesType.getBaseType());
        assertEquals(ArgumentType.TABLE, listOfTablesType.getElementType().getBaseType());
        assertEquals(ArgumentType.STRING,
                listOfTablesType.getElementType().getKeyType().getBaseType());
        assertEquals(ArgumentType.INTEGER,
                listOfTablesType.getElementType().getValueType().getBaseType());
    }

    @Test
    public void testParseTableWithListKeyThrows() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", Map.of("type", "list", "elementDefinition", "string"));
        tableSpec.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(tableSpec);
        });
    }

    @Test
    public void testParseTableWithNestedTableKeyThrows() {
        Map<String, Object> innerTable = new HashMap<>();
        innerTable.put("type", "table");
        innerTable.put("keyDefinition", "string");
        innerTable.put("valueDefinition", "integer");

        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", innerTable);
        tableSpec.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(tableSpec);
        });
    }

    @Test
    public void testParseNestedGroupLikeTableType() {
        // Table<String, Table<String, List<Integer>>> — group whose values are groups of lists
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", "integer");

        Map<String, Object> innerTableSpec = new HashMap<>();
        innerTableSpec.put("type", "table");
        innerTableSpec.put("keyDefinition", "string");
        innerTableSpec.put("valueDefinition", listSpec);

        Map<String, Object> outerTableSpec = new HashMap<>();
        outerTableSpec.put("type", "table");
        outerTableSpec.put("keyDefinition", "string");
        outerTableSpec.put("valueDefinition", innerTableSpec);

        TypeDefinition outerType = TypeParser.parse(outerTableSpec);
        assertEquals(ArgumentType.TABLE, outerType.getBaseType());
        assertEquals(ArgumentType.STRING, outerType.getKeyType().getBaseType());

        TypeDefinition innerType = outerType.getValueType();
        assertEquals(ArgumentType.TABLE, innerType.getBaseType());
        assertEquals(ArgumentType.STRING, innerType.getKeyType().getBaseType());
        assertEquals(ArgumentType.LIST, innerType.getValueType().getBaseType());
        assertEquals(ArgumentType.INTEGER,
                innerType.getValueType().getElementType().getBaseType());
    }

    @Test
    public void testParseDelegation() {
        // Verify TypeDefinition.parse() delegates to TypeParser.parse()
        TypeDefinition viaDelegation = TypeDefinition.parse("string");
        TypeDefinition viaDirect = TypeParser.parse("string");
        assertEquals(viaDelegation.getBaseType(), viaDirect.getBaseType());
    }
}
