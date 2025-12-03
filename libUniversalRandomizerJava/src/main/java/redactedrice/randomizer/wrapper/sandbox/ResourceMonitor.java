package redactedrice.randomizer.wrapper.sandbox;

import org.luaj.vm2.LuaValue;

import java.util.concurrent.Callable;

// Handles resource limiting (memory and execution time) for Lua script execution
public class ResourceMonitor {
    // How often we check is not a huge priority since these are not meant
    // to be strict guidelines but only general safety mechanims
    // 1/5 of a second is arbitrarily chosen
    public static final long DEFAULT_MONITORING_INTERVAL_MS = 200;
    public static final long MONITORING_INTERVAL_DISABLED = -1;

    // Default to 5 seconds arbitrarily but not too long so the timeout
    // tests don't take forever. These scripts should not take long to run
    public static final long DEFAULT_MAX_EXECUTION_TIME_MS = 5 * 1000;
    public static final long MAX_EXECUTION_TIME_DISABLED = -1;

    // Default to 100MB arbitrarily. Should be plenty big for what we are
    // intending on doing
    public static final long DEFAULT_MAX_MEMORY_BYTES = 100 * 1024 * 1024;
    public static final long MAX_MEMORY_DISABLED = -1;

    // I don't care too much about the specific memory usage so default to
    // false. This means a script coulr use more memory than the limit if we
    // do a GC after starting the script but that's fine as the intent is just
    // to keep it from overwhelming the device
    private static final boolean DEFAULT_GC_BEFORE_SNAPSHOT = false;

    private long maxMemoryBytes;
    private long maxExecutionTimeMs;
    private long monitoringIntervalMs;
    private boolean gcBeforeSnapshot;

    public ResourceMonitor() {
        this.maxMemoryBytes = DEFAULT_MAX_MEMORY_BYTES;
        this.maxExecutionTimeMs = DEFAULT_MAX_EXECUTION_TIME_MS;
        this.monitoringIntervalMs = DEFAULT_MONITORING_INTERVAL_MS;
        this.gcBeforeSnapshot = DEFAULT_GC_BEFORE_SNAPSHOT;
    }

    public boolean isMonitoringEnabled() {
        return (maxMemoryBytes != MAX_MEMORY_DISABLED
                || maxExecutionTimeMs != MAX_EXECUTION_TIME_DISABLED)
                && monitoringIntervalMs != MONITORING_INTERVAL_DISABLED;
    }

    public LuaValue executeWithMonitoring(Callable<LuaValue> executionTask, String context)
            throws TimeoutException {
        // Skip monitoring if both limits are disabled or monitoring interval is disabled
        if (!isMonitoringEnabled()) {
            return executeWithoutMonitoring(executionTask);
        }

        long memoryBefore = getInitialMemoryUsage();
        long executionStartTime = System.currentTimeMillis();

        ExecutionState state = new ExecutionState();
        Thread executionThread = createExecutionThread(executionTask, state);
        Thread monitoringThread = createMonitoringThread(state, executionThread, memoryBefore,
                executionStartTime, context);

        try {
            waitForThreadCompletion(executionThread, state, context);
        } catch (InterruptedException e) {
            executionThread.interrupt();
            if (monitoringThread != null) {
                monitoringThread.interrupt();
            }
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lua execution interrupted", e);
        } finally {
            if (monitoringThread != null) {
                monitoringThread.interrupt();
                try {
                    monitoringThread.join(1000); // Wait up to 1 second for cleanup
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return checkExecutionResult(state);
    }

    public LuaValue executeWithoutMonitoring(Callable<LuaValue> executionTask) {
        try {
            return executionTask.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Lua execution error: " + e.getMessage(), e);
        }
    }

    private Thread createExecutionThread(Callable<LuaValue> executionTask, ExecutionState state) {
        Thread executionThread = new Thread(() -> {
            try {
                LuaValue value = executionTask.call();
                state.result.set(value);
            } catch (Throwable e) {
                state.executionException.set(e);
            } finally {
                state.executionComplete.set(true);
            }
        }, "LuaSandbox-Executor");
        executionThread.setDaemon(true);
        executionThread.start();
        return executionThread;
    }

    private Thread createMonitoringThread(ExecutionState state, Thread executionThread,
            long memoryBefore, long executionStartTime, String context) {
        // Only create monitoring thread if limits are enabled. Shouldn't be
        // called if its not enabled but just in case
        if (!isMonitoringEnabled()) {
            return null;
        }

        Thread monitoringThread = new Thread(() -> {
            monitorExecution(state, executionThread, memoryBefore, executionStartTime, context);
        }, "LuaSandbox-Monitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
        return monitoringThread;
    }

    private void monitorExecution(ExecutionState state, Thread executionThread, long memoryBefore,
            long executionStartTime, String context) {
        while (!state.executionComplete.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(monitoringIntervalMs);

                if (state.executionComplete.get()) {
                    break;
                }

                checkTimeoutLimit(state, executionThread, executionStartTime, context);
                checkMemoryLimit(state, executionThread, memoryBefore, context);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void checkTimeoutLimit(ExecutionState state, Thread executionThread,
            long executionStartTime, String context) {
        if (maxExecutionTimeMs == MAX_EXECUTION_TIME_DISABLED) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - executionStartTime;
        if (elapsedTime > maxExecutionTimeMs) {
            executionThread.interrupt();
            state.executionComplete.set(true);
            state.timeoutException.set(new TimeoutException(String.format(
                    "Execution timeout exceeded during %s. Elapsed: %d ms, Limit: %d ms", context,
                    elapsedTime, maxExecutionTimeMs)));
        }
    }

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private long getInitialMemoryUsage() {
        // Clear up memory and wait a bit for it if set to do so
        if (gcBeforeSnapshot && maxMemoryBytes != MAX_MEMORY_DISABLED) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return getCurrentMemoryUsage();
    }

    private void checkMemoryLimit(ExecutionState state, Thread executionThread, long memoryBefore,
            String context) {
        if (maxMemoryBytes == MAX_MEMORY_DISABLED) {
            return;
        }

        // Compary current memory to initial memory to determine if the
        // script exceeds its allotment. Note its possible for this to be
        // negative if memory is freed after the initial snapshot
        long memoryNow = getCurrentMemoryUsage();
        long memoryUsed = memoryNow - memoryBefore;

        if (memoryUsed > maxMemoryBytes) {
            executionThread.interrupt();
            state.executionComplete.set(true);
            state.memoryException.set(new MemoryLimitExceededException(String.format(
                    "Memory limit exceeded during %s. Used: %d bytes (%.2f MB), Limit: %d bytes (%.2f MB)",
                    context, memoryUsed, memoryUsed / (1024.0 * 1024.0), maxMemoryBytes,
                    maxMemoryBytes / (1024.0 * 1024.0))));
        }
    }

    private void waitForThreadCompletion(Thread executionThread, ExecutionState state,
            String context) throws InterruptedException {
        if (maxExecutionTimeMs != MAX_EXECUTION_TIME_DISABLED) {
            executionThread.join(maxExecutionTimeMs);
            // Check if timeout was exceeded - i.e. thread is still running
            if (executionThread.isAlive()) {
                executionThread.interrupt();
                state.executionComplete.set(true);
                // Wait a moment for monitoring thread to set exception if it hasn't already
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // If monitoring thread didn't set timeout exception, set it here
                if (state.timeoutException.get() == null) {
                    state.timeoutException.set(new TimeoutException(
                            String.format("Execution timeout exceeded during %s. Limit: %d ms",
                                    context, maxExecutionTimeMs)));
                }
            }
        } else {
            executionThread.join();
        }
    }

    private LuaValue checkExecutionResult(ExecutionState state) {
        // Check if timeout was exceeded
        if (state.timeoutException.get() != null) {
            throw state.timeoutException.get();
        }

        // Check if memory limit was exceeded
        if (state.memoryException.get() != null) {
            throw state.memoryException.get();
        }

        // Check if execution threw an exception
        if (state.executionException.get() != null) {
            Throwable cause = state.executionException.get();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Lua execution error: " + cause.getMessage(), cause);
        }

        return state.result.get();
    }

    // Getters
    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    public boolean doGcBeforeSnapshot() {
        return gcBeforeSnapshot;
    }

    public long getMonitoringIntervalMs() {
        return monitoringIntervalMs;
    }

    // Setters
    public void setMaxExecutionTimeMs(long maxExecutionTimeMs) {
        if (maxExecutionTimeMs <= 0 && maxExecutionTimeMs != MAX_EXECUTION_TIME_DISABLED) {
            throw new IllegalArgumentException(
                    "maxExecutionTimeMs must be positive or MAX_EXECUTION_TIME_DISABLED to disable");
        }
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }

    public void disableMaxExecutionTime() {
        setMaxExecutionTimeMs(MAX_EXECUTION_TIME_DISABLED);
    }

    public void setMaxMemoryLimiting(long maxMemoryBytes, boolean gcBeforeSnapshot) {
        if (maxMemoryBytes < 0 && maxMemoryBytes != MAX_MEMORY_DISABLED) {
            throw new IllegalArgumentException(
                    "maxMemoryBytes must be positive or MAX_MEMORY_DISABLED to disable");
        }
        this.maxMemoryBytes = maxMemoryBytes;
        this.gcBeforeSnapshot = gcBeforeSnapshot;
    }

    public void disableMaxMemory() {
        setMaxMemoryLimiting(MAX_MEMORY_DISABLED, gcBeforeSnapshot);
    }

    public void setMonitoringIntervalMs(long monitoringIntervalMs) {
        if (monitoringIntervalMs <= 0 && monitoringIntervalMs != MONITORING_INTERVAL_DISABLED) {
            throw new IllegalArgumentException(
                    "monitoringIntervalMs must be positive or MONITORING_INTERVAL_DISABLED to disable");
        }
        this.monitoringIntervalMs = monitoringIntervalMs;
    }

    public void disableMonitoringInterval() {
        setMonitoringIntervalMs(MONITORING_INTERVAL_DISABLED);
    }
}
