package redactedrice.randomizer.lua.arguments;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TypeParserTest {

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
    public void testParseListAndTableTypes() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", "integer");
        TypeDefinition listType = TypeParser.parse(listSpec);
        assertEquals(ArgumentType.LIST, listType.getBaseType());
        assertEquals(ArgumentType.INTEGER, listType.getElementType().getBaseType());

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
    public void testParseInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(123));
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(new HashMap<>()));
    }

    @Test
    public void testParseEnumTypeVariants() {
        Map<String, Object> withEnumName = new HashMap<>();
        withEnumName.put("type", "enum");
        withEnumName.put("enumName", "EntityType");
        TypeDefinition namedEnum = TypeParser.parse(withEnumName);
        assertEquals(ArgumentType.ENUM, namedEnum.getBaseType());
        assertEquals("EntityType", namedEnum.getEnumName());

        Map<String, Object> withConstraint = new HashMap<>();
        withConstraint.put("type", "enum");
        withConstraint.put("constraint", "EntityType");
        TypeDefinition constrainedEnum = TypeParser.parse(withConstraint);
        assertEquals(ArgumentType.ENUM, constrainedEnum.getBaseType());

        Map<String, Object> withExclude = new HashMap<>();
        withExclude.put("type", "enum");
        withExclude.put("constraint", "EnergyType");
        withExclude.put("exclude", java.util.Arrays.asList("COLORLESS", "UNUSED_TYPE"));
        TypeDefinition excludedEnum = TypeParser.parse(withExclude);
        assertEquals("EnergyType", excludedEnum.getEnumName());
        assertEquals(ConstraintType.ENUM, excludedEnum.getConstraint().getType());
        assertEquals(2, excludedEnum.getConstraint().getExcludedValues().size());
        assertTrue(excludedEnum.getConstraint().validate("FIRE", ArgumentType.ENUM));
        assertFalse(excludedEnum.getConstraint().validate("COLORLESS", ArgumentType.ENUM));

        Map<String, Object> nestedConstraint = new HashMap<>();
        nestedConstraint.put("type", "enum");
        Map<String, Object> constraintMap = new HashMap<>();
        constraintMap.put("name", "EnergyType");
        constraintMap.put("exclude", java.util.Arrays.asList("UNUSED_TYPE"));
        nestedConstraint.put("constraint", constraintMap);
        TypeDefinition nested = TypeParser.parse(nestedConstraint);
        assertEquals("EnergyType", nested.getEnumName());
        assertFalse(nested.getConstraint().validate("UNUSED_TYPE", ArgumentType.ENUM));

        Map<String, Object> missingName = new HashMap<>();
        missingName.put("type", "enum");
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(missingName));
    }

    @Test
    public void testParseListTypeMissingElementDefinition() {
        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(listSpec));
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
    public void testParseWithConstraints() {
        Map<String, Object> rangeConstraint = Map.of("type", "range", "min", 1.0, "max", 100.0);
        Map<String, Object> rangeType = Map.of("type", "integer", "constraint", rangeConstraint);
        assertNotNull(TypeParser.parse(rangeType).getConstraint());

        Map<String, Object> stepConstraint = Map.of("type", "discrete_range", "min", 0.0, "max",
                100.0, "step", 10.0);
        Map<String, Object> stepType = Map.of("type", "integer", "constraint", stepConstraint);
        assertNotNull(TypeParser.parse(stepType).getConstraint());

        Map<String, Object> enumConstraint = Map.of("type", "enum", "values", java.util.Arrays.asList("A", "B", "C"));
        Map<String, Object> enumType = Map.of("type", "string", "constraint", enumConstraint);
        assertNotNull(TypeParser.parse(enumType).getConstraint());
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
    public void testParseTableKeyValidation() {
        Map<String, Object> listKeyTable = new HashMap<>();
        listKeyTable.put("type", "table");
        listKeyTable.put("keyDefinition",
                Map.of("type", "list", "elementDefinition", "string"));
        listKeyTable.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(listKeyTable));

        Map<String, Object> innerTable = Map.of("type", "table", "keyDefinition", "string",
                "valueDefinition", "integer");
        Map<String, Object> nestedKeyTable = new HashMap<>();
        nestedKeyTable.put("type", "table");
        nestedKeyTable.put("keyDefinition", innerTable);
        nestedKeyTable.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(nestedKeyTable));
    }

    @Test
    public void testParseNestedGroupLikeTableType() {
        // Table<String, Table<String, List<Integer>>> - group whose values are groups
        // of lists
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
    public void testParseTupleType() {
        Map<String, Object> weightField = new HashMap<>();
        weightField.put("name", "weight");
        weightField.put("definition", "integer");

        Map<String, Object> shapeField = new HashMap<>();
        shapeField.put("name", "shape");
        shapeField.put("definition", "string");

        Map<String, Object> tupleSpec = new HashMap<>();
        tupleSpec.put("type", "tuple");
        tupleSpec.put("fields", List.of(weightField, shapeField));

        TypeDefinition tupleType = TypeParser.parse(tupleSpec);
        assertEquals(ArgumentType.TUPLE, tupleType.getBaseType());
        assertEquals("weight", tupleType.getTupleField(0).name());
        assertEquals(ArgumentType.INTEGER, tupleType.getTupleField(0).type().getBaseType());
        assertEquals("shape", tupleType.getTupleField(1).name());
        assertEquals(ArgumentType.STRING, tupleType.getTupleField(1).type().getBaseType());
    }

    @Test
    public void testParseTupleWithTableField() {
        Map<String, Object> shapeTableSpec = new HashMap<>();
        shapeTableSpec.put("type", "table");
        shapeTableSpec.put("keyDefinition", "string");
        shapeTableSpec.put("valueDefinition", "integer");
        shapeTableSpec.put("fixedKeys", List.of("BASIC", "STAGE_1", "STAGE_2"));

        Map<String, Object> weightField = new HashMap<>();
        weightField.put("name", "weight");
        weightField.put("definition", "integer");

        Map<String, Object> shapeField = new HashMap<>();
        shapeField.put("name", "shape");
        shapeField.put("definition", shapeTableSpec);

        Map<String, Object> tupleSpec = new HashMap<>();
        tupleSpec.put("type", "tuple");
        tupleSpec.put("fields", List.of(weightField, shapeField));

        TypeDefinition tupleType = TypeParser.parse(tupleSpec);
        assertEquals(ArgumentType.TABLE, tupleType.getTupleField(1).type().getBaseType());
        assertTrue(tupleType.getTupleField(1).type().hasFixedKeys());
    }

    @Test
    public void testParseTupleWithListFieldThrows() {
        Map<String, Object> tagsSpec = new HashMap<>();
        tagsSpec.put("type", "list");
        tagsSpec.put("elementDefinition", "string");

        Map<String, Object> labelField = new HashMap<>();
        labelField.put("name", "label");
        labelField.put("definition", "string");

        Map<String, Object> tagsField = new HashMap<>();
        tagsField.put("name", "tags");
        tagsField.put("definition", tagsSpec);

        Map<String, Object> tupleSpec = new HashMap<>();
        tupleSpec.put("type", "tuple");
        tupleSpec.put("fields", List.of(labelField, tagsField));

        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(tupleSpec));
    }

    @Test
    public void testParseTableWithFixedKeys() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        tableSpec.put("valueDefinition", "integer");
        tableSpec.put("fixedKeys", List.of("BASIC", "STAGE_1", "STAGE_2"));

        TypeDefinition tableType = TypeParser.parse(tableSpec);
        assertTrue(tableType.hasFixedKeys());
        assertEquals(List.of("BASIC", "STAGE_1", "STAGE_2"), tableType.getFixedKeys());
    }

    @Test
    public void testParseTableWithFixedValues() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        tableSpec.put("valueDefinition", "integer");
        tableSpec.put("fixedKeys", List.of("BASIC", "STAGE_1", "STAGE_2"));
        tableSpec.put("fixedValues", Map.of("BASIC", 1));

        TypeDefinition tableType = TypeParser.parse(tableSpec);
        assertTrue(tableType.isFixedValue("BASIC"));
        assertEquals(1, tableType.getFixedValues().get("BASIC"));
    }

    @Test
    public void testParseTableFixedValuesRequiresFixedKeys() {
        Map<String, Object> tableSpec = new HashMap<>();
        tableSpec.put("type", "table");
        tableSpec.put("keyDefinition", "string");
        tableSpec.put("valueDefinition", "integer");
        tableSpec.put("fixedValues", Map.of("BASIC", 1));
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(tableSpec));
    }

    @Test
    public void testParseDelegation() {
        // Verify TypeDefinition.parse() delegates to TypeParser.parse()
        TypeDefinition viaDelegation = TypeDefinition.parse("string");
        TypeDefinition viaDirect = TypeParser.parse("string");
        assertEquals(viaDelegation.getBaseType(), viaDirect.getBaseType());
    }
}
