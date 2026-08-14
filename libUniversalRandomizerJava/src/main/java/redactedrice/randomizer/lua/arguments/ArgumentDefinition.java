package redactedrice.randomizer.lua.arguments;

import redactedrice.randomizer.context.EnumRegistry;

// defines a single argument for a lua module including its type and default value
public class ArgumentDefinition {
    String name;
    String displayName;
    String description;
    TypeDefinition typeDefinition;
    Object defaultValue;

    public ArgumentDefinition(String name, TypeDefinition typeDefinition, Object defaultValue) {
        this(name, null, null, typeDefinition, defaultValue);
    }

    public ArgumentDefinition(String name, String displayName, TypeDefinition typeDefinition,
            Object defaultValue) {
        this(name, displayName, null, typeDefinition, defaultValue);
    }

    public ArgumentDefinition(String name, String displayName, String description,
            TypeDefinition typeDefinition, Object defaultValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument name cannot be null or empty");
        }
        if (typeDefinition == null) {
            throw new IllegalArgumentException("Argument type cannot be null");
        }

        this.name = name;
        this.displayName = displayName != null && !displayName.isBlank() ? displayName.trim()
                : null;
        this.description = description != null && !description.isBlank() ? description.trim()
                : null;
        this.typeDefinition = typeDefinition;
        this.defaultValue = defaultValue;
    }

    public boolean validate(Object value, EnumRegistry enumRegistry) {
        if (value == null && defaultValue == null) {
            return false;
        }

        try {
            Object resolved = value != null ? value : defaultValue;
            Object converted =
                    ArgumentConverter.convertAndValidate(resolved, typeDefinition, enumRegistry);
            return converted != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Object convertAndValidate(Object value, EnumRegistry enumRegistry) {
        Object resolved = value != null ? value : defaultValue;
        if (resolved == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        return ArgumentConverter.convertAndValidate(resolved, typeDefinition, enumRegistry);
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public String getRegisteredDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public TypeDefinition getTypeDefinition() {
        return typeDefinition;
    }

    public ArgumentConstraint getConstraint() {
        // Generally should always use the enforced constraint
        return typeDefinition.getEnforcedConstraint();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return String.format(
                "ArgumentDefinition{name='%s', displayName='%s', type=%s, constraint=%s, default=%s}",
                name, displayName, typeDefinition,
                typeDefinition.getEnforcedConstraint().getDescription(), defaultValue);
    }
}
