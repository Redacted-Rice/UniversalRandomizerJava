package redactedrice.randomizer.lua.arguments;

/** One named field in a fixed two field tuple type. */
public record TupleFieldDefinition(String name, TypeDefinition type) {
    public TupleFieldDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tuple field name cannot be empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Tuple field type cannot be null");
        }
        if (type.isList() || type.isTuple()) {
            throw new IllegalArgumentException(
                    "Tuple field '" + name + "' cannot be a list or nested tuple, got: " + type);
        }
    }
}
