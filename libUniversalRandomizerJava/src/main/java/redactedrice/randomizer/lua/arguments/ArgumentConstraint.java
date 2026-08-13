package redactedrice.randomizer.lua.arguments;

import redactedrice.randomizer.context.EnumDefinition;

import java.util.ArrayList;
import java.util.List;

// holds constraints on argument values like ranges or allowed values
public class ArgumentConstraint {
    ConstraintType type;
    Double min;
    Double max;
    Double step;
    List<Object> allowedValues;
    List<Object> excludedValues;

    private ArgumentConstraint(ConstraintType type) {
        this.type = type;
    }

    public static ArgumentConstraint any() {
        return new ArgumentConstraint(ConstraintType.ANY);
    }

    public static ArgumentConstraint range(double min, double max) {
        ArgumentConstraint constraint = new ArgumentConstraint(ConstraintType.RANGE);
        constraint.min = min;
        constraint.max = max;
        return constraint;
    }

    public static ArgumentConstraint discreteRange(double min, double max, double step) {
        ArgumentConstraint constraint = new ArgumentConstraint(ConstraintType.DISCRETE_RANGE);
        constraint.min = min;
        constraint.max = max;
        constraint.step = step;
        return constraint;
    }

    public static ArgumentConstraint enumValues(List<Object> values) {
        return enumFilter(values, null);
    }

    public static ArgumentConstraint enumExclusions(List<Object> excluded) {
        return enumFilter(null, excluded);
    }

    // Allowlist and/or denylist for enum/string choices. Null lists are ignored.
    public static ArgumentConstraint enumFilter(List<Object> allowed, List<Object> excluded) {
        ArgumentConstraint constraint = new ArgumentConstraint(ConstraintType.ENUM);
        if (allowed != null) {
            constraint.allowedValues = new ArrayList<>(allowed);
        }
        if (excluded != null) {
            constraint.excludedValues = new ArrayList<>(excluded);
        }
        return constraint;
    }

    public boolean validate(Object value, ArgumentType baseType) {
        return validate(value, baseType, null);
    }

    // enumDef lets allow/exclude entries match via display labels or case insensitive names
    public boolean validate(Object value, ArgumentType baseType, EnumDefinition enumDef) {
        // null values always fail validation
        if (value == null) {
            return false;
        }

        // validate based on constraint type
        switch (type) {
            case ANY:
                // no constraints everything is valid
                return true;

            case RANGE:
                // numeric range check
                if (!(value instanceof Number)) {
                    return false;
                }
                double numValue = ((Number) value).doubleValue();
                return numValue >= min && numValue <= max;

            case DISCRETE_RANGE:
                // numeric range with step like 0, 5, 10, 15
                if (!(value instanceof Number)) {
                    return false;
                }
                double discreteValue = ((Number) value).doubleValue();
                if (discreteValue < min || discreteValue > max) {
                    return false;
                }
                // check if value is min + (n * step)
                double diff = discreteValue - min;
                double remainder = diff % step;
                // use small epsilon for floating point comparison
                return Math.abs(remainder) < 0.0001 || Math.abs(remainder - step) < 0.0001;

            case ENUM:
                if (allowedValues != null && !allowedValues.isEmpty()) {
                    boolean allowed = false;
                    for (Object candidate : allowedValues) {
                        if (matchesEnumConstraintValue(candidate, value, enumDef)) {
                            allowed = true;
                            break;
                        }
                    }
                    if (!allowed) {
                        return false;
                    }
                }
                if (excludedValues != null) {
                    for (Object excluded : excludedValues) {
                        if (matchesEnumConstraintValue(excluded, value, enumDef)) {
                            return false;
                        }
                    }
                }
                return true;

            default:
                return false;
        }
    }

    // Filters a registered enum's value list by this constraint's allow/exclude lists.
    // Non-ENUM constraints return the input unchanged.
    public List<String> filterEnumValues(List<String> allValues) {
        return filterEnumValues(allValues, null);
    }

    public List<String> filterEnumValues(List<String> allValues, EnumDefinition enumDef) {
        if (allValues == null || type != ConstraintType.ENUM) {
            return allValues;
        }
        List<String> filtered = new ArrayList<>();
        for (String value : allValues) {
            if (validate(value, ArgumentType.ENUM, enumDef)) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    // Prefer enum registry resolution so display labels and odd casing still match canonicals.
    static boolean matchesEnumConstraintValue(Object constraintEntry, Object value,
            EnumDefinition enumDef) {
        if (constraintEntry == null || value == null) {
            return false;
        }
        if (enumDef != null) {
            String entryCanonical = enumDef.resolveCanonicalValue(String.valueOf(constraintEntry));
            String valueCanonical = enumDef.resolveCanonicalValue(String.valueOf(value));
            if (entryCanonical != null && entryCanonical.equals(valueCanonical)) {
                return true;
            }
        }
        if (constraintEntry.toString().equalsIgnoreCase(value.toString())) {
            return true;
        }
        return matchesAllowedValue(constraintEntry, value);
    }

    static boolean matchesAllowedValue(Object allowed, Object value) {
        if (allowed.equals(value) || allowed.toString().equals(value.toString())) {
            return true;
        }
        // Compare numerically when both sides are Number to handle Lua's habit of converting
        // whole-number doubles to Integer (e.g. constraint value 2 vs UI-supplied Double 2.0)
        return allowed instanceof Number && value instanceof Number
                && ((Number) allowed).doubleValue() == ((Number) value).doubleValue();
    }

    public String getDescription() {
        switch (type) {
            case ANY:
                return "any value";
            case RANGE:
                return String.format("range [%.2f, %.2f]", min, max);
            case DISCRETE_RANGE:
                return String.format("discrete range [%.2f, %.2f] with step %.2f", min, max, step);
            case ENUM:
                StringBuilder description = new StringBuilder();
                if (allowedValues != null && !allowedValues.isEmpty()) {
                    description.append("one of: ").append(allowedValues);
                }
                if (excludedValues != null && !excludedValues.isEmpty()) {
                    if (description.length() > 0) {
                        description.append("; ");
                    }
                    description.append("excluding: ").append(excludedValues);
                }
                if (description.length() == 0) {
                    return "enum values";
                }
                return description.toString();
            default:
                return "unknown constraint";
        }
    }

    // Getters
    public ConstraintType getType() {
        return type;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getStep() {
        return step;
    }

    public List<Object> getAllowedValues() {
        return allowedValues != null ? new ArrayList<>(allowedValues) : null;
    }

    public List<Object> getExcludedValues() {
        return excludedValues != null ? new ArrayList<>(excludedValues) : null;
    }
}
