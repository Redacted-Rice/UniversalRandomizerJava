package redactedrice.randomizer.lua.sandbox;

// Exception thrown when Lua script exceeds memory limit
public class MemoryLimitExceededException extends RuntimeException {
    public MemoryLimitExceededException(String message) {
        super(message);
    }
}
