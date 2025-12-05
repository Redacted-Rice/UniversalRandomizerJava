package redactedrice.randomizer.lua.arguments;

import java.util.*;

// type definition that supports primitives lists maps and enums
// can handle nested types like list of maps or map of lists
public class TypeDefinition {
    public enum BaseType {
        STRING, INTEGER, DOUBLE, BOOLEAN, ENUM, LIST, MAP, GROUP
    }

    BaseType baseType;
    String enumName;
    TypeDefinition elementType;
    TypeDefinition keyType;
    TypeDefinition valueType;
    ArgumentConstraint constraint;

    private TypeDefinition(BaseType baseType, String enumName, TypeDefinition elementType,
            TypeDefinition keyType, TypeDefinition valueType, ArgumentConstraint constraint) {
        this.baseType = baseType;
        this.enumName = enumName;
        this.elementType = elementType;
        this.keyType = keyType;
        this.valueType = valueType;
        this.constraint = constraint != null ? constraint : ArgumentConstraint.any();
    }

    // Factory methods for primitive types
    public static TypeDefinition string() {
        return new TypeDefinition(BaseType.STRING, null, null, null, null, null);
    }

    public static TypeDefinition string(ArgumentConstraint constraint) {
        return new TypeDefinition(BaseType.STRING, null, null, null, null, constraint);
    }

    public static TypeDefinition integer() {
        return new TypeDefinition(BaseType.INTEGER, null, null, null, null, null);
    }

    public static TypeDefinition integer(ArgumentConstraint constraint) {
        return new TypeDefinition(BaseType.INTEGER, null, null, null, null, constraint);
    }

    public static TypeDefinition doubleType() {
        return new TypeDefinition(BaseType.DOUBLE, null, null, null, null, null);
    }

    public static TypeDefinition doubleType(ArgumentConstraint constraint) {
        return new TypeDefinition(BaseType.DOUBLE, null, null, null, null, constraint);
    }

    public static TypeDefinition bool() {
        return new TypeDefinition(BaseType.BOOLEAN, null, null, null, null, null);
    }

    public static TypeDefinition bool(ArgumentConstraint constraint) {
        return new TypeDefinition(BaseType.BOOLEAN, null, null, null, null, constraint);
    }

    // Factory method for enum type
    public static TypeDefinition enumType(String enumName) {
        if (enumName == null || enumName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        return new TypeDefinition(BaseType.ENUM, enumName.trim(), null, null, null, null);
    }

    // Factory method for list type
    public static TypeDefinition listOf(TypeDefinition elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("Element type cannot be null");
        }
        return new TypeDefinition(BaseType.LIST, null, elementType, null, null, null);
    }

    public static TypeDefinition mapOf(TypeDefinition keyType, TypeDefinition valueType) {
        if (keyType == null || valueType == null) {
            throw new IllegalArgumentException("Key and value types cannot be null");
        }
        return new TypeDefinition(BaseType.MAP, null, null, keyType, valueType, null);
    }

    public static TypeDefinition groupOf(TypeDefinition keyType, TypeDefinition listValueType) {
        if (keyType == null || listValueType == null) {
            throw new IllegalArgumentException("Key and value types cannot be null");
        }
        // value type should be a list
        return new TypeDefinition(BaseType.GROUP, null, null, keyType, listValueType, null);
    }

    public static TypeDefinition parse(Object typeSpec) {
        // type specs are either simple strings like integer or complex maps like {type: list
        // elementDefinition: integer}
        if (typeSpec instanceof String) {
            // simple type string
            return parseSimpleType((String) typeSpec);
        } else if (typeSpec instanceof Map) {
            // complex type with nesting
            return parseComplexType((Map<?, ?>) typeSpec);
        } else {
            throw new IllegalArgumentException("Invalid type specification: " + typeSpec);
        }
    }

    private static TypeDefinition parseSimpleType(String typeStr) {
        // handle simple type strings
        switch (typeStr.toLowerCase()) {
            case "string":
                return string();
            case "integer":
            case "int":
                return integer();
            case "double":
            case "number":
                return doubleType();
            case "boolean":
            case "bool":
                return bool();
            default:
                throw new IllegalArgumentException("Unknown type: " + typeStr);
        }
    }

    @SuppressWarnings("unchecked")
    private static TypeDefinition parseComplexType(Map<?, ?> typeMap) {
        // complex types are maps with a type field and other type specific fields
        String baseTypeStr = (String) typeMap.get("type");
        if (baseTypeStr == null) {
            throw new IllegalArgumentException("Type map must have 'type' field");
        }

        // extract constraint if present for primatives
        ArgumentConstraint constraint = null;
        Object constraintObj = typeMap.get("constraint");
        if (constraintObj != null) {
            constraint = parseConstraint(constraintObj, baseTypeStr);
        }

        // parse based on type
        switch (baseTypeStr.toLowerCase()) {
            case "string":
                return constraint != null ? string(constraint) : string();
            case "integer":
            case "int":
                return constraint != null ? integer(constraint) : integer();
            case "double":
            case "number":
                return constraint != null ? doubleType(constraint) : doubleType();
            case "boolean":
            case "bool":
                return constraint != null ? bool(constraint) : bool();

            case "enum":
                // for enum constraint is the enum name
                String enumName = constraintObj instanceof String ? (String) constraintObj
                        : (String) typeMap.get("enumName");
                if (enumName == null) {
                    throw new IllegalArgumentException(
                            "Enum type must specify enum name via 'constraint' or 'enumName'");
                }
                return enumType(enumName);

            case "list":
                // lists need an elementdefinition field specifying the element type
                Object elementSpec = typeMap.get("elementDefinition");
                if (elementSpec == null) {
                    throw new IllegalArgumentException(
                            "List type must specify 'elementDefinition'");
                }
                return listOf(parse(elementSpec));

            case "map":
                // maps need keydefinition and valuedefinition fields
                Object keySpec = typeMap.get("keyDefinition");
                Object valueSpec = typeMap.get("valueDefinition");
                if (keySpec == null || valueSpec == null) {
                    throw new IllegalArgumentException(
                            "Map type must specify 'keyDefinition' and 'valueDefinition'");
                }
                return mapOf(parse(keySpec), parse(valueSpec));

            case "group":
                // map where values are lists automatically converted to lua randomizer group
                Object groupKeySpec = typeMap.get("keyDefinition");
                Object listElementDefSpec = typeMap.get("listElementDefinition");

                if (groupKeySpec == null) {
                    throw new IllegalArgumentException("Group type must specify 'keyDefinition'");
                }

                if (listElementDefSpec == null) {
                    throw new IllegalArgumentException(
                            "Group type must specify 'listElementDefinition' (element type)");
                }

                // parse key type and element type
                TypeDefinition keyTypeDef = parse(groupKeySpec);
                // listelementdefinition is the element type
                // automatically wrap it in a list for the value type
                TypeDefinition elementTypeDef = parse(listElementDefSpec);
                TypeDefinition valueTypeDef = listOf(elementTypeDef);

                return groupOf(keyTypeDef, valueTypeDef);

            default:
                throw new IllegalArgumentException("Unknown type: " + baseTypeStr);
        }
    }

    private static ArgumentConstraint parseConstraint(Object constraintObj, String typeStr) {
        // constraints can be basic types strings or complex maps
        if (constraintObj instanceof String) {
            // for enum type constraint is just the enum name
            if ("enum".equalsIgnoreCase(typeStr)) {
                return ArgumentConstraint.any();
            }
            // just treate it as any
            return ArgumentConstraint.any();
        }

        // parse complex constraint from map
        if (constraintObj instanceof Map) {
            Map<?, ?> constraintMap = (Map<?, ?>) constraintObj;
            String constraintType = (String) constraintMap.get("type");

            if (constraintType == null) {
                return ArgumentConstraint.any();
            }

            // parse based on constraint type
            switch (constraintType.toLowerCase()) {
                case "any":
                    return ArgumentConstraint.any();

                case "range":
                    // Assumes min is less than max
                    double min = ((Number) constraintMap.get("min")).doubleValue();
                    double max = ((Number) constraintMap.get("max")).doubleValue();
                    return ArgumentConstraint.range(min, max);

                case "discrete_range":
                case "discreterange":
                    // value must be min + (n * step)
                    double dMin = ((Number) constraintMap.get("min")).doubleValue();
                    double dMax = ((Number) constraintMap.get("max")).doubleValue();
                    double step = ((Number) constraintMap.get("step")).doubleValue();
                    return ArgumentConstraint.discreteRange(dMin, dMax, step);

                case "enum":
                    // Enum constraint with explicit values
                    Object valuesObj = constraintMap.get("values");
                    if (valuesObj instanceof List) {
                        List<?> valuesList = (List<?>) valuesObj;
                        return ArgumentConstraint.enumValues(new ArrayList<>(valuesList));
                    }
                    throw new IllegalArgumentException("Enum constraint must have 'values' list");

                default:
                    return ArgumentConstraint.any();
            }
        }

        return ArgumentConstraint.any();
    }

    // Getters
    public BaseType getBaseType() {
        return baseType;
    }

    public String getEnumName() {
        return enumName;
    }

    public TypeDefinition getElementType() {
        return elementType;
    }

    public TypeDefinition getKeyType() {
        return keyType;
    }

    public TypeDefinition getValueType() {
        return valueType;
    }

    public boolean isPrimitive() {
        return baseType == BaseType.STRING || baseType == BaseType.INTEGER
                || baseType == BaseType.DOUBLE || baseType == BaseType.BOOLEAN;
    }

    public boolean isEnum() {
        return baseType == BaseType.ENUM;
    }

    public boolean isList() {
        return baseType == BaseType.LIST;
    }

    public boolean isMap() {
        return baseType == BaseType.MAP;
    }

    public boolean isComplex() {
        return isList() || isMap();
    }

    public ArgumentConstraint getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        switch (baseType) {
            case STRING:
                return "String";
            case INTEGER:
                return "Integer";
            case DOUBLE:
                return "Double";
            case BOOLEAN:
                return "Boolean";
            case ENUM:
                return "Enum<" + enumName + ">";
            case LIST:
                return "List<" + elementType + ">";
            case MAP:
                return "Map<" + keyType + ", " + valueType + ">";
            case GROUP:
                return "Group<" + keyType + ", " + valueType + ">";
            default:
                return "Unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeDefinition that = (TypeDefinition) o;
        return baseType == that.baseType && Objects.equals(enumName, that.enumName)
                && Objects.equals(elementType, that.elementType)
                && Objects.equals(keyType, that.keyType)
                && Objects.equals(valueType, that.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType, enumName, elementType, keyType, valueType);
    }
}
