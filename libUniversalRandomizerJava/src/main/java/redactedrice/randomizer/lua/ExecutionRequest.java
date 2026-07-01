package redactedrice.randomizer.lua;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to execute a module with specific arguments and seed offset. The offset is
 * added to the run's base seed at execution time.
 */
public final class ExecutionRequest {
    private final String moduleName;
    private final Map<String, Object> arguments;
    private final int seedOffset;
    private final boolean explicitSeedOffset;
    // Pre/post scripts intentionally omit arguments and seed offsets
    private final boolean script;

    // private constructor. Use static factories instead
    private ExecutionRequest(String moduleName, Map<String, Object> arguments, int seedOffset,
            boolean explicitSeedOffset, boolean script) {
        this.moduleName = Objects.requireNonNull(moduleName, "Module name cannot be null");
        this.arguments = arguments != null ? Map.copyOf(arguments) : Map.of();
        this.seedOffset = seedOffset;
        this.explicitSeedOffset = explicitSeedOffset;
        this.script = script;
    }

    // Copies the module's defaultSeedOffset (or name-hash offset) for use at execution time
    public static ExecutionRequest forModule(Module module, Map<String, Object> arguments) {
        Objects.requireNonNull(module, "Module cannot be null");
        return new ExecutionRequest(module.getName(), arguments, module.getSeedOffset(), false,
                false);
    }

    // Sets seed offset to the passed value
    public static ExecutionRequest forModuleWithSeedOffset(String moduleName,
            Map<String, Object> arguments, int seedOffset) {
        return new ExecutionRequest(moduleName, arguments, seedOffset, true, false);
    }

    public static ExecutionRequest forModuleWithSeedOffset(Module module,
            Map<String, Object> arguments, int seedOffset) {
        Objects.requireNonNull(module, "Module cannot be null");
        return forModuleWithSeedOffset(module.getName(), arguments, seedOffset);
    }

    // Pre/post scripts intentionally omit arguments and seed offsets
    public static ExecutionRequest forScript(Module script) {
        Objects.requireNonNull(script, "Script cannot be null");
        return new ExecutionRequest(script.getName(), Map.of(), 0, false, true);
    }

    public boolean isScript() {
        return script;
    }

    public boolean hasExplicitSeedOffset() {
        return explicitSeedOffset;
    }

    public int resolveAbsoluteSeed(int baseSeed) {
        if (script) {
            throw new IllegalStateException(
                    "Scripts do not use seeds: " + moduleName);
        }
        return baseSeed + seedOffset;
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
     * Gets the seed offset to use for randomization.
     *
     * @return the seed offset
     */
    public int getSeedOffset() {
        return seedOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExecutionRequest that = (ExecutionRequest) o;
        return seedOffset == that.seedOffset && explicitSeedOffset == that.explicitSeedOffset
                && script == that.script && moduleName.equals(that.moduleName)
                && arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, arguments, seedOffset, explicitSeedOffset, script);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ExecutionRequest{");
        sb.append("moduleName='").append(moduleName).append('\'');
        if (script) {
            sb.append(", script=true");
        }
        if (!arguments.isEmpty()) {
            sb.append(", arguments=").append(arguments);
        }
        if (!script) {
            sb.append(", seedOffset=").append(seedOffset);
            if (explicitSeedOffset) {
                sb.append(" (explicit)");
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
