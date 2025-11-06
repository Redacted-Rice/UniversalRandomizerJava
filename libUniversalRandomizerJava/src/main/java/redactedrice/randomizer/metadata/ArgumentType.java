package redactedrice.randomizer.metadata;

// supported argument types for lua module parameters
// TODO later add support for more basic types
public enum ArgumentType {
    STRING, INTEGER, DOUBLE, BOOLEAN;

    public static ArgumentType fromLuaString(String luaType) {
        if (luaType == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        String normalized = luaType.toLowerCase().trim();
        switch (normalized) {
            case "string":
                return STRING;
            case "integer":
            case "int":
                return INTEGER;
            case "double":
            case "number":
            case "float":
                return DOUBLE;
            case "boolean":
            case "bool":
                return BOOLEAN;
            default:
                throw new IllegalArgumentException("Unsupported type: " + luaType);
        }
    }

    public Object convertValue(Object value) {
        if (value == null) {
            return null;
        }

        try {
            switch (this) {
                case STRING:
                    return value.toString();
                case INTEGER:
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                case DOUBLE:
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                case BOOLEAN:
                    if (value instanceof Boolean) {
                        return value;
                    }
                    return Boolean.parseBoolean(value.toString());
                default:
                    throw new IllegalArgumentException("Unknown type: " + this);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert value '" + value + "' to " + this,
                    e);
        }
    }
}
