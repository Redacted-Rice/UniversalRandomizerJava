package redactedrice.randomizer.wrapper;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to execute a module with specific arguments and seed.
 * Allows the same module to be executed multiple times with different configurations.
 */
public final class ExecutionRequest {
    private final String moduleName;
    private final Map<String, Object> arguments;
    private final Integer seed;

    /**
     * Creates a new module execution request.
     *
     * @param moduleName the name of the module to execute (must not be null)
     * @param arguments  the arguments to pass to the module (null treated as empty map)
     * @param seed       the seed to use for randomization (null uses module's default seed offset)
     */
    public ExecutionRequest(String moduleName, Map<String, Object> arguments, Integer seed) {
        this.moduleName = Objects.requireNonNull(moduleName, "Module name cannot be null");
        this.arguments = arguments != null ? Map.copyOf(arguments) : Map.of();
        this.seed = seed;
    }

    /**
     * Creates a new module execution request with no seed (uses module's default).
     *
     * @param moduleName the name of the module to execute
     * @param arguments  the arguments to pass to the module
     */
    public ExecutionRequest(String moduleName, Map<String, Object> arguments) {
        this(moduleName, arguments, null);
    }

    /**
     * Creates a new module execution request with no arguments and no seed.
     *
     * @param moduleName the name of the module to execute
     */
    public ExecutionRequest(String moduleName) {
        this(moduleName, null, null);
    }

    /**
     * Gets the name of the module to execute.
     *
     * @return the module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Gets the arguments to pass to the module.
     * Returns an immutable map.
     *
     * @return the arguments map (never null, may be empty)
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Gets the seed to use for randomization.
     *
     * @return the seed, or null if module's default seed offset should be used
     */
    public Integer getSeed() {
        return seed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExecutionRequest that = (ExecutionRequest) o;
        return moduleName.equals(that.moduleName) && arguments.equals(that.arguments)
                && Objects.equals(seed, that.seed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, arguments, seed);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ExecutionRequest{");
        sb.append("moduleName='").append(moduleName).append('\'');
        if (!arguments.isEmpty()) {
            sb.append(", arguments=").append(arguments);
        }
        if (seed != null) {
            sb.append(", seed=").append(seed);
        }
        sb.append('}');
        return sb.toString();
    }
}
