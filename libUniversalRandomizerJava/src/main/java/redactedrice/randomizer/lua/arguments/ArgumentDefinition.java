package redactedrice.randomizer.lua.arguments;

import redactedrice.randomizer.context.EnumContext;

// defines a single argument for a lua module including its type and default value
public class ArgumentDefinition {
    String name;
    TypeDefinition typeDefinition;
    Object defaultValue;

    public ArgumentDefinition(String name, TypeDefinition typeDefinition, Object defaultValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument name cannot be null or empty");
        }
        if (typeDefinition == null) {
            throw new IllegalArgumentException("Argument type cannot be null");
        }

        this.name = name;
        this.typeDefinition = typeDefinition;
        this.defaultValue = defaultValue;
    }

    public boolean validate(Object value, EnumContext enumContext) {
        // if value is null but we have a default we will use the default
        if (value == null && defaultValue != null) {
            return true;
        }
        // if value is null and no default then its not valid
        if (value == null) {
            return false;
        }

        // try to convert and validate the value
        try {
            Object converted = TypeValidator.convertAndValidate(value, typeDefinition, enumContext);
            return converted != null;
        } catch (Exception e) {
            // any exception during validation means the value is invalid
            return false;
        }
    }

    public Object convertAndValidate(Object value, EnumContext enumContext) {
        // if no value provided use the default
        if (value == null && defaultValue != null) {
            return defaultValue;
        }

        // convert and validate using typevalidator
        return TypeValidator.convertAndValidate(value, typeDefinition, enumContext);
    }

    // Getters
    public String getName() {
        return name;
    }

    public TypeDefinition getTypeDefinition() {
        return typeDefinition;
    }

    public ArgumentConstraint getConstraint() {
        return typeDefinition.getConstraint();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return String.format("ArgumentDefinition{name='%s', type=%s, constraint=%s, default=%s}",
                name, typeDefinition, typeDefinition.getConstraint().getDescription(),
                defaultValue);
    }
}
