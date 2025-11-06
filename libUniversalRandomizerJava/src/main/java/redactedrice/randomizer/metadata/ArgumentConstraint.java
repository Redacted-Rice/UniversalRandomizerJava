package redactedrice.randomizer.metadata;

import java.util.ArrayList;
import java.util.List;

// holds constraints on argument values like ranges or allowed values
public class ArgumentConstraint {
    public enum ConstraintType {
        ANY, // Any value allowed
        RANGE, // Numeric range with min/max
        DISCRETE_RANGE, // Numeric range with min/max/step
        ENUM // Enumerated values
    }

    ConstraintType type;
    Double min;
    Double max;
    Double step;
    List<Object> allowedValues;

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
        ArgumentConstraint constraint = new ArgumentConstraint(ConstraintType.ENUM);
        constraint.allowedValues = new ArrayList<>(values);
        return constraint;
    }

    public boolean validate(Object value, ArgumentType argumentType) {
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
                // value must be in the allowed list
                for (Object allowed : allowedValues) {
                    if (allowed.equals(value) || allowed.toString().equals(value.toString())) {
                        return true;
                    }
                }
                return false;

            default:
                return false;
        }
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
                return "one of: " + allowedValues;
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
}
