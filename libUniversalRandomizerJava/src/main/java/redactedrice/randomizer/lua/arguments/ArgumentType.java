package redactedrice.randomizer.lua.arguments;

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
}
