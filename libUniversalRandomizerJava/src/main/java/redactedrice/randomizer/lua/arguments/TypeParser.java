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
                return parseEnumType(typeMap, constraintObj);

            case "list":
                // lists need an elementdefinition field specifying the element type
                Object elementSpec = typeMap.get("elementDefinition");
                if (elementSpec == null) {
                    throw new IllegalArgumentException(
                            "List type must specify 'elementDefinition'");
                }
                return TypeDefinition.listOf(parse(elementSpec));

            case "table":
                // tables need keyDefinition and valueDefinition fields
                Object keySpec = typeMap.get("keyDefinition");
                Object valueSpec = typeMap.get("valueDefinition");
                if (keySpec == null || valueSpec == null) {
                    throw new IllegalArgumentException(
                            "Table type must specify 'keyDefinition' and 'valueDefinition'");
                }
                List<String> fixedKeys = parseFixedKeys(typeMap.get("fixedKeys"));
                return TypeDefinition.tableOf(parse(keySpec), parse(valueSpec), fixedKeys,
                        parseFixedValues(typeMap.get("fixedValues"), fixedKeys));

            case "tuple":
                return parseTupleType(typeMap);

            default:
                throw new IllegalArgumentException("Unknown type: " + baseTypeStr);
        }
    }

    private static List<String> parseFixedKeys(Object fixedKeysObj) {
        if (fixedKeysObj == null) {
            return List.of();
        }
        if (!(fixedKeysObj instanceof List<?> fixedKeys)) {
            throw new IllegalArgumentException("Table 'fixedKeys' must be a list of strings");
        }
        List<String> result = new ArrayList<>();
        for (Object key : fixedKeys) {
            if (!(key instanceof String str) || str.isBlank()) {
                throw new IllegalArgumentException(
                        "Table 'fixedKeys' entries must be non-empty strings");
            }
            result.add(str);
        }
        return result;
    }

    private static Map<String, Object> parseFixedValues(Object fixedValuesObj,
            List<String> fixedKeys) {
        if (fixedValuesObj == null) {
            return Map.of();
        }
        if (!(fixedValuesObj instanceof Map<?, ?> fixedValues)) {
            throw new IllegalArgumentException("Table 'fixedValues' must be a map");
        }
        if (fixedValues.isEmpty()) {
            return Map.of();
        }
        if (fixedKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Table 'fixedValues' requires 'fixedKeys' to be specified");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : fixedValues.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException(
                        "Table 'fixedValues' keys must be non-empty strings");
            }
            if (!fixedKeys.contains(key)) {
                throw new IllegalArgumentException(
                        "Table 'fixedValues' key '" + key + "' must appear in 'fixedKeys'");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static TypeDefinition parseTupleType(Map<?, ?> typeMap) {
        Object fieldsObj = typeMap.get("fields");
        if (!(fieldsObj instanceof List<?> fields) || fields.size() != 2) {
            throw new IllegalArgumentException("Tuple type must specify exactly two 'fields'");
        }
        TupleFieldDefinition[] parsed = new TupleFieldDefinition[2];
        for (int i = 0; i < 2; i++) {
            Object fieldSpec = fields.get(i);
            if (!(fieldSpec instanceof Map<?, ?> fieldMap)) {
                throw new IllegalArgumentException("Tuple field must be a map with 'name' and 'definition'");
            }
            Object nameObj = fieldMap.get("name");
            Object definitionObj = fieldMap.get("definition");
            if (!(nameObj instanceof String name) || name.isBlank()) {
                throw new IllegalArgumentException("Tuple field must have a non-empty 'name'");
            }
            if (definitionObj == null) {
                throw new IllegalArgumentException("Tuple field '" + name + "' must have 'definition'");
            }
            parsed[i] = new TupleFieldDefinition(name, parse(definitionObj));
        }
        return TypeDefinition.tupleOf(parsed[0], parsed[1]);
    }

    private static TypeDefinition parseEnumType(Map<?, ?> typeMap, Object constraintObj) {
        String enumName = null;
        List<Object> allowed = asObjectList(typeMap.get("values"));
        List<Object> excluded = asObjectList(typeMap.get("exclude"));

        if (constraintObj instanceof String) {
            enumName = (String) constraintObj;
        } else if (constraintObj instanceof Map) {
            Map<?, ?> constraintMap = (Map<?, ?>) constraintObj;
            Object nameObj = constraintMap.get("name");
            if (nameObj == null) {
                nameObj = constraintMap.get("enumName");
            }
            if (nameObj instanceof String) {
                enumName = (String) nameObj;
            }
            if (allowed == null) {
                allowed = asObjectList(constraintMap.get("values"));
            }
            if (excluded == null) {
                excluded = asObjectList(constraintMap.get("exclude"));
            }
        } else if (typeMap.get("enumName") instanceof String) {
            enumName = (String) typeMap.get("enumName");
        }

        if (enumName == null) {
            throw new IllegalArgumentException(
                    "Enum type must specify enum name via 'constraint' or 'enumName'");
        }

        ArgumentConstraint enumConstraint = null;
        if ((allowed != null && !allowed.isEmpty()) || (excluded != null && !excluded.isEmpty())) {
            enumConstraint = ArgumentConstraint.enumFilter(allowed, excluded);
        }
        return TypeDefinition.enumType(enumName, enumConstraint);
    }

    private static List<Object> asObjectList(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        return new ArrayList<>((List<?>) value);
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
                // Enum name/exclude maps are handled in parseEnumType, not as a generic
                // constraint
                if ("enum".equalsIgnoreCase(typeStr)) {
                    return ArgumentConstraint.any();
                }
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
                    // Enum constraint with explicit values and optional exclusions
                    Object valuesObj = constraintMap.get("values");
                    Object excludeObj = constraintMap.get("exclude");
                    List<Object> allowed =
                            valuesObj instanceof List ? new ArrayList<>((List<?>) valuesObj) : null;
                    List<Object> excluded = excludeObj instanceof List
                            ? new ArrayList<>((List<?>) excludeObj)
                            : null;
                    if ((allowed == null || allowed.isEmpty())
                            && (excluded == null || excluded.isEmpty())) {
                        throw new IllegalArgumentException(
                                "Enum constraint must have 'values' and/or 'exclude' list");
                    }
                    return ArgumentConstraint.enumFilter(allowed, excluded);

                default:
                    return ArgumentConstraint.any();
            }
        }

        return ArgumentConstraint.any();
    }
}
