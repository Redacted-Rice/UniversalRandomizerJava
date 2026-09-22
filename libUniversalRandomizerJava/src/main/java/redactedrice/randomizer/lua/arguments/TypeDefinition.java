package redactedrice.randomizer.lua.arguments;

import java.util.*;

// type definition that supports primitives, lists, tables, tuples, and enums
// can handle nested types like list of tables or table of lists
public class TypeDefinition {
    private final ArgumentType baseType;
    private final String enumName;
    private final TypeDefinition elementType;
    private final TypeDefinition keyType;
    private final TypeDefinition valueType;
    private final List<TupleFieldDefinition> tupleFields;
    private final List<String> fixedKeys;
    private final Map<String, Object> fixedValues;
    private final ArgumentConstraint constraint;

    private TypeDefinition(ArgumentType baseType, String enumName, TypeDefinition elementType,
            TypeDefinition keyType, TypeDefinition valueType,
            List<TupleFieldDefinition> tupleFields, List<String> fixedKeys,
            Map<String, Object> fixedValues, ArgumentConstraint constraint) {
        this.baseType = baseType;
        this.enumName = enumName;
        this.elementType = elementType;
        this.keyType = keyType;
        this.valueType = valueType;
        this.tupleFields = tupleFields != null ? List.copyOf(tupleFields) : List.of();
        this.fixedKeys = fixedKeys != null ? List.copyOf(fixedKeys) : List.of();
        this.fixedValues = fixedValues != null ? Map.copyOf(fixedValues) : Map.of();
        this.constraint = constraint != null ? constraint : ArgumentConstraint.any();
    }

    // Factory methods for primitive types
    public static TypeDefinition string() {
        return new TypeDefinition(ArgumentType.STRING, null, null, null, null, null, null, null,
                null);
    }

    public static TypeDefinition string(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.STRING, null, null, null, null, null, null, null,
                constraint);
    }

    public static TypeDefinition integer() {
        return new TypeDefinition(ArgumentType.INTEGER, null, null, null, null, null, null, null,
                null);
    }

    public static TypeDefinition integer(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.INTEGER, null, null, null, null, null, null, null,
                constraint);
    }

    public static TypeDefinition doubleType() {
        return new TypeDefinition(ArgumentType.DOUBLE, null, null, null, null, null, null, null,
                null);
    }

    public static TypeDefinition doubleType(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.DOUBLE, null, null, null, null, null, null, null,
                constraint);
    }

    public static TypeDefinition bool() {
        return new TypeDefinition(ArgumentType.BOOLEAN, null, null, null, null, null, null, null,
                null);
    }

    public static TypeDefinition bool(ArgumentConstraint constraint) {
        return new TypeDefinition(ArgumentType.BOOLEAN, null, null, null, null, null, null, null,
                constraint);
    }

    // Factory method for enum type
    public static TypeDefinition enumType(String enumName) {
        return enumType(enumName, null);
    }

    public static TypeDefinition enumType(String enumName, ArgumentConstraint constraint) {
        if (enumName == null || enumName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        return new TypeDefinition(ArgumentType.ENUM, enumName.trim(), null, null, null, null, null,
                null, constraint);
    }

    // Factory method for list type
    public static TypeDefinition listOf(TypeDefinition elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("Element type cannot be null");
        }
        return new TypeDefinition(ArgumentType.LIST, null, elementType, null, null, null, null,
                null, null);
    }

    public static TypeDefinition tableOf(TypeDefinition keyType, TypeDefinition valueType) {
        return tableOf(keyType, valueType, List.of());
    }

    public static TypeDefinition tableOf(TypeDefinition keyType, TypeDefinition valueType,
            List<String> fixedKeys) {
        return tableOf(keyType, valueType, fixedKeys, Map.of());
    }

    public static TypeDefinition tableOf(TypeDefinition keyType, TypeDefinition valueType,
            List<String> fixedKeys, Map<String, Object> fixedValues) {
        if (keyType == null || valueType == null) {
            throw new IllegalArgumentException("Key and value types cannot be null");
        }
        if (!keyType.isSingleValueType()) {
            throw new IllegalArgumentException(
                    "Table key type must be a single-value type (string, integer, double, boolean, or enum), got: "
                            + keyType);
        }
        return new TypeDefinition(ArgumentType.TABLE, null, null, keyType, valueType, null,
                fixedKeys, fixedValues, null);
    }

    public static TypeDefinition tupleOf(String field0Name, TypeDefinition field0Type,
            String field1Name, TypeDefinition field1Type) {
        return tupleOf(new TupleFieldDefinition(field0Name, field0Type),
                new TupleFieldDefinition(field1Name, field1Type));
    }

    public static TypeDefinition tupleOf(TupleFieldDefinition field0, TupleFieldDefinition field1) {
        if (field0 == null || field1 == null) {
            throw new IllegalArgumentException("Tuple fields cannot be null");
        }
        return new TypeDefinition(ArgumentType.TUPLE, null, null, null, null,
                List.of(field0, field1), null, null, null);
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

    public List<TupleFieldDefinition> getTupleFields() {
        return tupleFields;
    }

    public TupleFieldDefinition getTupleField(int index) {
        return tupleFields.get(index);
    }

    public List<String> getFixedKeys() {
        return fixedKeys;
    }

    public boolean hasFixedKeys() {
        return isTable() && !fixedKeys.isEmpty();
    }

    public Map<String, Object> getFixedValues() {
        return fixedValues;
    }

    public boolean hasFixedValues() {
        return isTable() && !fixedValues.isEmpty();
    }

    public boolean isFixedValue(String key) {
        return fixedValues.containsKey(key);
    }

    public boolean isPrimitive() {
        return baseType == ArgumentType.STRING || baseType == ArgumentType.INTEGER
                || baseType == ArgumentType.DOUBLE || baseType == ArgumentType.BOOLEAN;
    }

    public boolean isEnum() {
        return baseType == ArgumentType.ENUM;
    }

    // Primitive or enum - the only types allowed as table keys (values may be any
    // type).
    public boolean isSingleValueType() {
        return isPrimitive() || isEnum();
    }

    public boolean isList() {
        return baseType == ArgumentType.LIST;
    }

    public boolean isTable() {
        return baseType == ArgumentType.TABLE;
    }

    public boolean isTuple() {
        return baseType == ArgumentType.TUPLE;
    }

    // True for TABLE rows (key/value columns) and LIST rows whose element is a
    // two-field Tuple.
    public boolean hasPairRows() {
        return isTable() || (isList() && elementType != null && elementType.isTuple());
    }

    public TypeDefinition getRowHeadType() {
        if (isTable()) {
            return keyType;
        }
        if (isList() && elementType != null && elementType.isTuple()) {
            return elementType.getTupleField(0).type();
        }
        throw new IllegalStateException("Type does not have pair rows: " + this);
    }

    public TypeDefinition getRowTailType() {
        if (isTable()) {
            return valueType;
        }
        if (isList() && elementType != null && elementType.isTuple()) {
            return elementType.getTupleField(1).type();
        }
        throw new IllegalStateException("Type does not have pair rows: " + this);
    }

    public TypeDefinition getCollectionValueType() {
        if (isTable()) {
            return valueType;
        }
        if (isList() && elementType != null && elementType.isTuple()) {
            return getRowTailType();
        }
        return elementType;
    }

    public boolean isComplex() {
        return isList() || isTable();
    }

    public ArgumentConstraint getConstraint() {
        return constraint;
    }

    // Constraint enforced at validation/UI time. Booleans are always ANY; strings
    // ignore
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
            case TUPLE:
                return "Tuple<" + tupleFields.get(0).type() + ", " + tupleFields.get(1).type()
                        + ">";
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
                && Objects.equals(valueType, that.valueType)
                && Objects.equals(tupleFields, that.tupleFields)
                && Objects.equals(fixedKeys, that.fixedKeys)
                && Objects.equals(fixedValues, that.fixedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType, enumName, elementType, keyType, valueType, tupleFields,
                fixedKeys, fixedValues);
    }
}
