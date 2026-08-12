package redactedrice.randomizer.lua.arguments;

import java.util.*;

// type definition that supports primitives, lists, tables, and enums
// can handle nested types like list of tables or table of lists
public class TypeDefinition {
    private final ArgumentType baseType;
    private final String enumName;
    private final TypeDefinition elementType;
    private final TypeDefinition keyType;
    private final TypeDefinition valueType;
    private final ArgumentConstraint constraint;

    private TypeDefinition(ArgumentType baseType, String enumName, TypeDefinition elementType,
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
        return new TypeDefinition(ArgumentType.STRING, null, null, null, null, null);
    }

    public static TypeDefinition string(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.STRING, null, null, null, null, constraint);
    }

    public static TypeDefinition integer() {
        return new TypeDefinition(ArgumentType.INTEGER, null, null, null, null, null);
    }

    public static TypeDefinition integer(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.INTEGER, null, null, null, null, constraint);
    }

    public static TypeDefinition doubleType() {
        return new TypeDefinition(ArgumentType.DOUBLE, null, null, null, null, null);
    }

    public static TypeDefinition doubleType(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.DOUBLE, null, null, null, null, constraint);
    }

    public static TypeDefinition bool() {
        return new TypeDefinition(ArgumentType.BOOLEAN, null, null, null, null, null);
    }

    public static TypeDefinition bool(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.BOOLEAN, null, null, null, null, constraint);
    }

    // Factory method for enum type
    public static TypeDefinition enumType(String enumName) {
        return enumType(enumName, null);
    }

    public static TypeDefinition enumType(String enumName, ArgumentConstraint constraint) {
        if (enumName == null || enumName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        return new TypeDefinition(ArgumentType.ENUM, enumName.trim(), null, null, null, constraint);
    }

    // Factory method for list type
    public static TypeDefinition listOf(TypeDefinition elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("Element type cannot be null");
        }
        return new TypeDefinition(ArgumentType.LIST, null, elementType, null, null, null);
    }

    public static TypeDefinition tableOf(TypeDefinition keyType, TypeDefinition valueType) {
        if (keyType == null || valueType == null) {
            throw new IllegalArgumentException("Key and value types cannot be null");
        }
        if (!keyType.isSingleValueType()) {
            throw new IllegalArgumentException(
                    "Table key type must be a single-value type (string, integer, double, boolean, or enum), got: "
                            + keyType);
        }
        return new TypeDefinition(ArgumentType.TABLE, null, null, keyType, valueType, null);
    }

    public static TypeDefinition parse(Object typeSpec) {
        return TypeParser.parse(typeSpec);
    }

    // Getters
    public ArgumentType getBaseType() {
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
        return baseType == ArgumentType.STRING || baseType == ArgumentType.INTEGER
                || baseType == ArgumentType.DOUBLE || baseType == ArgumentType.BOOLEAN;
    }

    public boolean isEnum() {
        return baseType == ArgumentType.ENUM;
    }

    // Primitive or enum — the only types allowed as table keys (values may be any type).
    public boolean isSingleValueType() {
        return isPrimitive() || isEnum();
    }

    public boolean isList() {
        return baseType == ArgumentType.LIST;
    }

    public boolean isTable() {
        return baseType == ArgumentType.TABLE;
    }

    public boolean isComplex() {
        return isList() || isTable();
    }

    public ArgumentConstraint getConstraint() {
        return constraint;
    }

    // Constraint enforced at validation/UI time. Booleans are always ANY; strings ignore
    // range/discrete constraints that do not apply to that type.
    public ArgumentConstraint getEnforcedConstraint() {
        if (!isPrimitive()) {
            return constraint;
        }
        if (baseType == ArgumentType.BOOLEAN) {
            return ArgumentConstraint.any();
        }
        if (baseType == ArgumentType.STRING
                && (constraint.getType() == ConstraintType.RANGE
                        || constraint.getType() == ConstraintType.DISCRETE_RANGE)) {
            return ArgumentConstraint.any();
        }
        return constraint;
    }

    public boolean declaresIgnoredConstraint() {
        if (!isPrimitive() || constraint.getType() == ConstraintType.ANY) {
            return false;
        }
        return getEnforcedConstraint().getType() == ConstraintType.ANY;
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
            case TABLE:
                return "Table<" + keyType + ", " + valueType + ">";
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
