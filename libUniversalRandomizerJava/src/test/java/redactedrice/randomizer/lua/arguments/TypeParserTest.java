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
    public void testParseMapType() {
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("keyDefinition", "string");
        mapSpec.put("valueDefinition", "integer");
        TypeDefinition mapType = TypeParser.parse(mapSpec);
        assertEquals(ArgumentType.MAP, mapType.getBaseType());
        assertEquals(ArgumentType.STRING, mapType.getKeyType().getBaseType());
        assertEquals(ArgumentType.INTEGER, mapType.getValueType().getBaseType());
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
    public void testParseMapTypeMissingKeyDefinition() {
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("valueDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(mapSpec);
        });
    }

    @Test
    public void testParseMapTypeMissingValueDefinition() {
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("keyDefinition", "string");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(mapSpec);
        });
    }

    @Test
    public void testParseGroupTypeMissingKeyDefinition() {
        Map<String, Object> groupSpec = new HashMap<>();
        groupSpec.put("type", "group");
        groupSpec.put("listElementDefinition", "integer");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(groupSpec);
        });
    }

    @Test
    public void testParseGroupTypeMissingListElementDefinition() {
        Map<String, Object> groupSpec = new HashMap<>();
        groupSpec.put("type", "group");
        groupSpec.put("keyDefinition", "string");
        assertThrows(IllegalArgumentException.class, () -> {
            TypeParser.parse(groupSpec);
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
    public void testParseGroupType() {
        Map<String, Object> groupSpec = new HashMap<>();
        groupSpec.put("type", "group");
        groupSpec.put("keyDefinition", "string");
        groupSpec.put("listElementDefinition", "integer");

        TypeDefinition groupType = TypeParser.parse(groupSpec);
        assertEquals(ArgumentType.GROUP, groupType.getBaseType());
        assertEquals(ArgumentType.STRING, groupType.getKeyType().getBaseType());
        assertEquals(ArgumentType.LIST, groupType.getValueType().getBaseType());
        assertEquals(ArgumentType.INTEGER,
                groupType.getValueType().getElementType().getBaseType());
    }

    @Test
    public void testParseNestedComplexTypes() {
        // List of maps: List<Map<String, Integer>>
        Map<String, Object> mapSpec = new HashMap<>();
        mapSpec.put("type", "map");
        mapSpec.put("keyDefinition", "string");
        mapSpec.put("valueDefinition", "integer");

        Map<String, Object> listSpec = new HashMap<>();
        listSpec.put("type", "list");
        listSpec.put("elementDefinition", mapSpec);

        TypeDefinition listOfMapsType = TypeParser.parse(listSpec);
        assertEquals(ArgumentType.LIST, listOfMapsType.getBaseType());
        assertEquals(ArgumentType.MAP, listOfMapsType.getElementType().getBaseType());
        assertEquals(ArgumentType.STRING,
                listOfMapsType.getElementType().getKeyType().getBaseType());
        assertEquals(ArgumentType.INTEGER,
                listOfMapsType.getElementType().getValueType().getBaseType());
    }

    @Test
    public void testParseDelegation() {
        // Verify TypeDefinition.parse() delegates to TypeParser.parse()
        TypeDefinition viaDelegation = TypeDefinition.parse("string");
        TypeDefinition viaDirect = TypeParser.parse("string");
        assertEquals(viaDelegation.getBaseType(), viaDirect.getBaseType());
    }
}
