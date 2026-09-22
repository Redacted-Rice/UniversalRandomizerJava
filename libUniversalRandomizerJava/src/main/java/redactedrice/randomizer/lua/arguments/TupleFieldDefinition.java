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
        if (type.isComplex() || type.isTuple()) {
            throw new IllegalArgumentException(
                    "Tuple field '" + name + "' must be a scalar or enum type, got: " + type);
        }
    }
}
