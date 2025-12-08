package redactedrice.randomizer.lua.arguments;

public enum ConstraintType {
    ANY, // Any value allowed
    RANGE, // Numeric range with min/max
    DISCRETE_RANGE, // Numeric range with min/max/step
    ENUM // Enumerated values
}
