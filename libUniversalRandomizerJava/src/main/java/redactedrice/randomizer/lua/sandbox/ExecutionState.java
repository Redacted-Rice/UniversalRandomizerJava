package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.LuaValue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Holds execution state shared between execution and monitoring threads
public class ExecutionState {
    public final AtomicReference<LuaValue> result = new AtomicReference<>();
    public final AtomicReference<Throwable> executionException = new AtomicReference<>();
    public final AtomicReference<MemoryLimitExceededException> memoryException =
            new AtomicReference<>();
    public final AtomicReference<TimeoutException> timeoutException = new AtomicReference<>();
    public final AtomicBoolean executionComplete = new AtomicBoolean(false);
}
