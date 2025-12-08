package redactedrice.randomizer.lua.arguments;

import java.util.*;

public class TypeParser {

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
                return TypeDefinition.string();
            case "integer":
            case "int":
                return TypeDefinition.integer();
            case "double":
            case "number":
                return TypeDefinition.doubleType();
            case "boolean":
            case "bool":
                return TypeDefinition.bool();
            default:
                throw new IllegalArgumentException("Unknown type: " + typeStr);
        }
    }

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
                return constraint != null ? TypeDefinition.string(constraint)
                        : TypeDefinition.string();
            case "integer":
            case "int":
                return constraint != null ? TypeDefinition.integer(constraint)
                        : TypeDefinition.integer();
            case "double":
            case "number":
                return constraint != null ? TypeDefinition.doubleType(constraint)
                        : TypeDefinition.doubleType();
            case "boolean":
            case "bool":
                return constraint != null ? TypeDefinition.bool(constraint) : TypeDefinition.bool();

            case "enum":
                // for enum constraint is the enum name
                String enumName = constraintObj instanceof String ? (String) constraintObj
                        : (String) typeMap.get("enumName");
                if (enumName == null) {
                    throw new IllegalArgumentException(
                            "Enum type must specify enum name via 'constraint' or 'enumName'");
                }
                return TypeDefinition.enumType(enumName);

            case "list":
                // lists need an elementdefinition field specifying the element type
                Object elementSpec = typeMap.get("elementDefinition");
                if (elementSpec == null) {
                    throw new IllegalArgumentException(
                            "List type must specify 'elementDefinition'");
                }
                return TypeDefinition.listOf(parse(elementSpec));

            case "map":
                // maps need keydefinition and valuedefinition fields
                Object keySpec = typeMap.get("keyDefinition");
                Object valueSpec = typeMap.get("valueDefinition");
                if (keySpec == null || valueSpec == null) {
                    throw new IllegalArgumentException(
                            "Map type must specify 'keyDefinition' and 'valueDefinition'");
                }
                return TypeDefinition.mapOf(parse(keySpec), parse(valueSpec));

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
                TypeDefinition valueTypeDef = TypeDefinition.listOf(elementTypeDef);

                return TypeDefinition.groupOf(keyTypeDef, valueTypeDef);

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
}
