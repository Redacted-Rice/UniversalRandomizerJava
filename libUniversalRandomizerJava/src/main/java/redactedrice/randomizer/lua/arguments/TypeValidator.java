package redactedrice.randomizer.lua.arguments;

import redactedrice.randomizer.context.EnumContext;
import redactedrice.randomizer.context.EnumDefinition;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;

// handles converting and validating argument values based on their type definitions
public class TypeValidator {
    public static Object convertAndValidate(Object value, TypeDefinition typeDef,
            EnumContext enumContext) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        // convert the value to the right type
        Object converted = convertValue(value, typeDef, enumContext);

        // check constraints for primitive types
        if (typeDef.isPrimitive()) {
            ArgumentConstraint constraint = typeDef.getConstraint();
            if (constraint != null && !validateConstraint(converted, constraint)) {
                throw new IllegalArgumentException(
                        String.format("Value '%s' does not satisfy constraint: %s", value,
                                constraint.getDescription()));
            }
        }

        return converted;
    }

    private static Object convertValue(Object value, TypeDefinition typeDef,
            EnumContext enumContext) {
        // call the right converter based on type
        switch (typeDef.getBaseType()) {
            case STRING:
                return convertToString(value);

            case INTEGER:
                return convertToInteger(value);

            case DOUBLE:
                return convertToDouble(value);

            case BOOLEAN:
                return convertToBoolean(value);

            case ENUM:
                // need enumcontext to look up valid enum values
                return convertToEnum(value, typeDef.getEnumName(), enumContext);

            case LIST:
                // recursively convert each element
                return convertToList(value, typeDef.getElementType(), enumContext);

            case MAP:
                // recursively convert keys and values
                return convertToMap(value, typeDef.getKeyType(), typeDef.getValueType(),
                        enumContext);

            case GROUP:
                // groups are just maps where values are lists
                // actual group object creation happens later in convertArgumentsToLuaTable
                return convertToMap(value, typeDef.getKeyType(), typeDef.getValueType(),
                        enumContext);

            default:
                throw new IllegalArgumentException("Unknown type: " + typeDef.getBaseType());
        }
    }

    private static String convertToString(Object value) {
        return value.toString();
    }

    private static Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to integer");
            }
        } else if (value instanceof LuaValue) {
            return ((LuaValue) value).toint();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getSimpleName() + " to integer");
    }

    private static Double convertToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to double");
            }
        } else if (value instanceof LuaValue) {
            return ((LuaValue) value).todouble();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getSimpleName() + " to double");
    }

    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            if (str.equals("true") || str.equals("yes") || str.equals("1")) {
                return true;
            } else if (str.equals("false") || str.equals("no") || str.equals("0")) {
                return false;
            }
            throw new IllegalArgumentException("Cannot convert '" + value + "' to boolean");
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof LuaValue) {
            return ((LuaValue) value).toboolean();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getSimpleName() + " to boolean");
    }

    private static String convertToEnum(Object value, String enumName, EnumContext enumContext) {
        if (enumContext == null) {
            throw new IllegalArgumentException("EnumContext required for enum type validation. "
                    + "This typically means the context was not properly initialized or passed to validation.");
        }

        // lookup the enum definition
        EnumDefinition enumDef = enumContext.getEnum(enumName);
        if (enumDef == null) {
            // build helpful error message with available enums
            Set<String> availableEnumsSet = enumContext.getEnumNames();
            List<String> availableEnums = new ArrayList<>(availableEnumsSet);
            Collections.sort(availableEnums);

            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Enum '").append(enumName).append("' not found in context");
            if (!availableEnums.isEmpty()) {
                errorMsg.append(". Available enums: ").append(availableEnums);
            } else {
                errorMsg.append(". No enums are registered in the context");
            }
            errorMsg.append(". This is likely due to mispelling enum name or not correctly ");
            errorMsg.append("registering the enum with context.registerEnum() in the onLoad() ");
            errorMsg.append("function of the module");
            throw new IllegalArgumentException(errorMsg.toString());
        }

        // check if the value is valid for this enum
        String strValue = value.toString();
        if (!enumDef.hasValue(strValue)) {
            throw new IllegalArgumentException(
                    String.format("Value '%s' is not valid for enum '%s'. Valid values: %s",
                            strValue, enumName, enumDef.getValues()));
        }

        return strValue;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> convertToList(Object value, TypeDefinition elementType,
            EnumContext enumContext) {
        List<Object> result = new ArrayList<>();

        // handle java lists
        if (value instanceof List) {
            List<?> sourceList = (List<?>) value;
            for (Object element : sourceList) {
                Object converted = convertValue(element, elementType, enumContext);
                result.add(converted);
            }
        } else if (value instanceof LuaTable) {
            // handle lua tables as lists
            LuaTable table = (LuaTable) value;
            int len = table.length();
            // lua arrays are 1indexed
            for (int i = 1; i <= len; i++) {
                LuaValue element = table.get(i);
                if (!element.isnil()) {
                    Object converted = convertValue(luaToJava(element), elementType, enumContext);
                    result.add(converted);
                }
            }
        } else if (value.getClass().isArray()) {
            // handle java arrays
            Object[] array = (Object[]) value;
            for (Object element : array) {
                Object converted = convertValue(element, elementType, enumContext);
                result.add(converted);
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot convert " + value.getClass().getSimpleName() + " to List");
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> convertToMap(Object value, TypeDefinition keyType,
            TypeDefinition valueType, EnumContext enumContext) {
        Map<Object, Object> result = new LinkedHashMap<>();

        if (value instanceof Map) {
            // convert java maps
            Map<?, ?> sourceMap = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                Object convertedKey = convertValue(entry.getKey(), keyType, enumContext);
                Object convertedValue = convertValue(entry.getValue(), valueType, enumContext);
                result.put(convertedKey, convertedValue);
            }
        } else if (value instanceof LuaTable) {
            // convert lua tables to maps
            LuaTable table = (LuaTable) value;
            LuaValue[] keys = table.keys();
            for (LuaValue key : keys) {
                LuaValue val = table.get(key);
                Object convertedKey = convertValue(luaToJava(key), keyType, enumContext);
                Object convertedValue = convertValue(luaToJava(val), valueType, enumContext);
                result.put(convertedKey, convertedValue);
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot convert " + value.getClass().getSimpleName() + " to Map");
        }

        return result;
    }

    private static Object luaToJava(LuaValue value) {
        if (value.isnil()) {
            return null;
        } else if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isint()) {
            return value.toint();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            // keep tables as lua for now since theyll be processed later
            return value;
        } else {
            return value.touserdata();
        }
    }

    private static boolean validateConstraint(Object value, ArgumentConstraint constraint) {
        if (constraint == null || constraint.getType() == ArgumentConstraint.ConstraintType.ANY) {
            return true;
        }

        switch (constraint.getType()) {
            case RANGE:
                // check if value is in min max range
                if (!(value instanceof Number)) {
                    return false;
                }
                double numValue = ((Number) value).doubleValue();
                return numValue >= constraint.getMin() && numValue <= constraint.getMax();

            case DISCRETE_RANGE:
                // check if value is in range and on the right step
                if (!(value instanceof Number)) {
                    return false;
                }
                double discreteValue = ((Number) value).doubleValue();
                if (discreteValue < constraint.getMin() || discreteValue > constraint.getMax()) {
                    return false;
                }
                // check if the value is on a valid step
                double diff = discreteValue - constraint.getMin();
                double remainder = diff % constraint.getStep();
                return Math.abs(remainder) < 0.0001
                        || Math.abs(remainder - constraint.getStep()) < 0.0001;

            case ENUM:
                // check if value is in the allowed list
                List<Object> allowed = constraint.getAllowedValues();
                for (Object allowedValue : allowed) {
                    if (allowedValue.equals(value)
                            || allowedValue.toString().equals(value.toString())) {
                        return true;
                    }
                }
                return false;

            default:
                return true;
        }
    }
}
