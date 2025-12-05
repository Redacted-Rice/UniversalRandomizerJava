package redactedrice.randomizer.lua;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to execute a module with specific arguments and seed. Allows the same module
 * to be executed multiple times with different configurations.
 */
public final class ExecutionRequest {
    private final String moduleName;
    private final Map<String, Object> arguments;
    private final int seed;

    // private constructor. Use static factories instead
    private ExecutionRequest(String moduleName, Map<String, Object> arguments, int seed) {
        this.moduleName = Objects.requireNonNull(moduleName, "Module name cannot be null");
        this.arguments = arguments != null ? Map.copyOf(arguments) : Map.of();
        this.seed = seed;
    }

    // Sets seed to the passed seed
    public static ExecutionRequest withSeed(String moduleName, Map<String, Object> arguments,
            int seed) {
        return new ExecutionRequest(moduleName, arguments, seed);
    }

    // Pulls the default seed offset from the module and offsets the passed seed with that
    public static ExecutionRequest withDefaultSeedOffset(String moduleName,
            Map<String, Object> arguments, int baseSeed, ModuleRegistry moduleRegistry) {
        Objects.requireNonNull(moduleRegistry, "ModuleRegistry cannot be null");
        Module module = moduleRegistry.getModule(moduleName);
        if (module == null) {
            throw new IllegalArgumentException("Module not found: " + moduleName);
        }
        int offset = module.getDefaultSeedOffset();
        int finalSeed = baseSeed + offset;
        return new ExecutionRequest(moduleName, arguments, finalSeed);
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
     * Gets the arguments to pass to the module. Returns an immutable map.
     *
     * @return the arguments map (never null, may be empty)
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Gets the seed to use for randomization.
     *
     * @return the seed
     */
    public int getSeed() {
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
                && seed == that.seed;
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
        sb.append(", seed=").append(seed);
        sb.append('}');
        return sb.toString();
    }
}
