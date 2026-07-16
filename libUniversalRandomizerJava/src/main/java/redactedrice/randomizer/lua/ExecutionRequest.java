package redactedrice.randomizer.lua;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to execute a module with specific arguments and seed offset. The offset is
 * added to the run's base seed at execution time for seeded modules only.
 */
public final class ExecutionRequest {
    private final String moduleId;
    private final Map<String, Object> arguments;
    private final int seedOffset;
    private final boolean explicitSeedOffset;
    // Pre/post scripts intentionally omit arguments and seed offsets
    private final boolean script;
    // Whether this module participates in seed configuration at execution time
    private final boolean seeded;

    // private constructor. Use static factories instead
    private ExecutionRequest(String moduleId, Map<String, Object> arguments, int seedOffset,
            boolean explicitSeedOffset, boolean script, boolean seeded) {
        this.moduleId = Objects.requireNonNull(moduleId, "Module id cannot be null");
        this.arguments = arguments != null ? Map.copyOf(arguments) : Map.of();
        this.seedOffset = seedOffset;
        this.explicitSeedOffset = explicitSeedOffset;
        this.script = script;
        this.seeded = seeded;
    }

    // Copies the module's defaultSeedOffset (or id-hash offset) for use at execution time
    public static ExecutionRequest forModule(Module module, Map<String, Object> arguments) {
        Objects.requireNonNull(module, "Module cannot be null");
        if (!module.isSeeded()) {
            return forUnseededModule(module, arguments);
        }
        return new ExecutionRequest(module.getId(), arguments, module.getSeedOffset(), false,
                false, true);
    }

    public static ExecutionRequest forUnseededModule(Module module,
            Map<String, Object> arguments) {
        Objects.requireNonNull(module, "Module cannot be null");
        return new ExecutionRequest(module.getId(), arguments, 0, false, false, false);
    }

    // Sets seed offset to the passed value
    public static ExecutionRequest forModuleWithSeedOffset(String moduleId,
            Map<String, Object> arguments, int seedOffset) {
        return new ExecutionRequest(moduleId, arguments, seedOffset, true, false, true);
    }

    public static ExecutionRequest forModuleWithSeedOffset(Module module,
            Map<String, Object> arguments, int seedOffset) {
        Objects.requireNonNull(module, "Module cannot be null");
        if (!module.isSeeded()) {
            return forUnseededModule(module, arguments);
        }
        return forModuleWithSeedOffset(module.getId(), arguments, seedOffset);
    }

    // Pre/post scripts intentionally omit arguments and seed offsets
    public static ExecutionRequest forScript(Module script) {
        Objects.requireNonNull(script, "Script cannot be null");
        return new ExecutionRequest(script.getId(), Map.of(), 0, false, true, false);
    }

    public boolean isScript() {
        return script;
    }

    public boolean usesSeed() {
        return seeded && !script;
    }

    public boolean hasExplicitSeedOffset() {
        return explicitSeedOffset;
    }

    public int resolveAbsoluteSeed(int baseSeed) {
        if (script) {
            throw new IllegalStateException(
                    "Scripts do not use seeds: " + moduleId);
        }
        if (!seeded) {
            throw new IllegalStateException(
                    "Unseeded modules do not use seeds: " + moduleId);
        }
        return baseSeed + seedOffset;
    }

    /**
     * Gets the id of the module to execute.
     *
     * @return the module id
     */
    public String getModuleId() {
        return moduleId;
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
                && script == that.script && seeded == that.seeded
                && moduleId.equals(that.moduleId) && arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleId, arguments, seedOffset, explicitSeedOffset, script, seeded);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ExecutionRequest{");
        sb.append("moduleId='").append(moduleId).append('\'');
        if (script) {
            sb.append(", script=true");
        }
        if (!arguments.isEmpty()) {
            sb.append(", arguments=").append(arguments);
        }
        if (!script) {
            sb.append(", seeded=").append(seeded);
            if (seeded) {
                sb.append(", seedOffset=").append(seedOffset);
                if (explicitSeedOffset) {
                    sb.append(" (explicit)");
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
