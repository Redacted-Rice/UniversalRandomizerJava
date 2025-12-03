package redactedrice.randomizer.wrapper.sandbox;

// Exception thrown when Lua script exceeds execution time limit
public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
    }
}
