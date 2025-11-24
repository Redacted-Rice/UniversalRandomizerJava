package redactedrice.randomizer.wrapper;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import redactedrice.randomizer.logger.LuaLogFunctions;

import org.luaj.vm2.lib.DebugLib;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// sandboxed lua environment that blocks dangerous functions and libraries
public class LuaSandbox {
    // Default to 100MB arbitrarily
    public static final long DEFAULT_MAX_MEMORY_BYTES = 100 * 1024 * 1024;
    private static final long MEMORY_CHECK_INTERVAL_MS = 200;

    Globals globals;
    List<Path> allowedRootDirectories;
    private final long maxMemoryBytes;


    public LuaSandbox(List<String> allowedRootDirectories) {
        this(allowedRootDirectories, DEFAULT_MAX_MEMORY_BYTES);
    }

    public LuaSandbox(List<String> allowedRootDirectories, long maxMemoryBytes) {
        if (allowedRootDirectories == null || allowedRootDirectories.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one allowed root directory must be provided");
        }

        this.allowedRootDirectories = new ArrayList<>();
        for (String dirPath : allowedRootDirectories) {
            if (dirPath == null || dirPath.trim().isEmpty()) {
                continue;
            }

            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new IllegalArgumentException(
                        "Allowed directory does not exist or is not a directory: " + dirPath);
            }

            this.allowedRootDirectories.add(Paths.get(dir.getAbsolutePath()).normalize());
        }

        if (this.allowedRootDirectories.isEmpty()) {
            throw new IllegalArgumentException("No valid allowed root directories provided");
        }

        if (maxMemoryBytes < 0 && maxMemoryBytes != -1) {
            throw new IllegalArgumentException("maxMemoryBytes must be positive or -1 to disable");
        }

        this.maxMemoryBytes = maxMemoryBytes;

        this.globals = createSandboxedGlobals();
    }

    public Globals getGlobals() {
        return globals;
    }

    private boolean isPathAllowed(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            Path requestedPath = Paths.get(filePath).toAbsolutePath().normalize();

            // Check if the requested path is within any allowed root directory
            for (Path allowedRoot : allowedRootDirectories) {
                Path normalizedAllowed = allowedRoot.toAbsolutePath().normalize();
                if (requestedPath.startsWith(normalizedAllowed)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            // If path resolution fails deny access
            return false;
        }
    }

    private Globals createSandboxedGlobals() {
        // start with standard lua libs including the compiler
        Globals globals = JsePlatform.standardGlobals();

        // load debug lib so we can get stack traces
        globals.load(new DebugLib());

        // remove dangerous stuff
        blockDangerousFunctions(globals);

        // restrict what can be required
        setupRestrictedRequire(globals);

        // add logger functions
        setupLoggerFunctions(globals);

        return globals;
    }

    private void blockDangerousFunctions(Globals globals) {
        // remove io os and luajava completely
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);

        // restrict debug table to only have traceback
        LuaValue debugLib = globals.get("debug");
        if (!debugLib.isnil() && debugLib.istable()) {
            LuaTable restrictedDebug = new LuaTable();
            LuaValue traceback = debugLib.get("traceback");
            if (!traceback.isnil()) {
                // keep traceback so we get good error messages
                restrictedDebug.set("traceback", traceback);
            }
            globals.set("debug", restrictedDebug);
        } else {
            globals.set("debug", LuaValue.NIL);
        }

        // remove dangerous base functions
        globals.set("dofile", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);
        globals.set("loadstring", LuaValue.NIL);

        // remove lua 5.1 environment manipulation
        globals.set("getfenv", LuaValue.NIL);
        globals.set("setfenv", LuaValue.NIL);

        // wrap loadfile to restrict access to allowed directories only
        final LuaValue originalLoadfile = globals.get("loadfile");
        globals.set("loadfile", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue filePath) {
                String path = filePath.tojstring();
                if (!isPathAllowed(path)) {
                    throw new RuntimeException("Access denied: Cannot load file '" + path
                            + "' - not in allowed directories");
                }
                return originalLoadfile.call(filePath);
            }
        });
    }

    private void setupRestrictedRequire(Globals globals) {
        // Create the modified package path based on our allowed directories
        LuaValue packageLib = globals.get("package");
        final String allowedPackagePath; // Declared here and set once below
        if (!packageLib.isnil()) {
            List<String> packagePaths = new ArrayList<>();

            for (Path allowedRoot : allowedRootDirectories) {
                File allowedDir = allowedRoot.toFile();
                File parentDir = allowedDir.getParentFile();
                if (parentDir != null) {
                    String parentPath = parentDir.getAbsolutePath().replace('\\', '/');
                    // Standard Lua module search patterns
                    packagePaths.add(parentPath + "/?.lua");
                    packagePaths.add(parentPath + "/?/init.lua");
                }
            }

            allowedPackagePath = String.join(";", packagePaths);
            packageLib.set("path", LuaValue.valueOf(allowedPackagePath));

            // block c library loading
            packageLib.set("cpath", LuaValue.valueOf(""));
        } else {
            // this should always be set but just in case package path is excluded already
            // don't add it back in
            allowedPackagePath = "";
        }

        // wrap require to validate that resolved paths are within allowed directories
        // Additionally reset package path before each require to prevent runtime modifications
        final LuaValue originalRequire = globals.get("require");
        final LuaValue packageLibFinal = packageLib;

        globals.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue moduleName) {
                String module = moduleName.tojstring();

                // Reset package path to the allowed value before each require call
                if (!packageLibFinal.isnil()) {
                    packageLibFinal.set("path", LuaValue.valueOf(allowedPackagePath));
                }

                try {
                    // Use package.searchpath to find where the module would be loaded from
                    if (!packageLibFinal.isnil()) {
                        LuaValue searchpath = packageLibFinal.get("searchpath");
                        if (!searchpath.isnil() && searchpath.isfunction()) {
                            LuaValue resolvedPath = searchpath.call(moduleName,
                                    LuaValue.valueOf(allowedPackagePath));
                            if (resolvedPath.isstring()) {
                                String filePath = resolvedPath.tojstring();
                                if (!isPathAllowed(filePath)) {
                                    throw new RuntimeException("Access denied: Cannot load module '"
                                            + module + "' - resolved path '" + filePath
                                            + "' is not in allowed directories");
                                }
                            }
                        }
                    }
                } catch (SecurityException e) {
                    // TODO: Revisit what to throw and what to catch
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    // Not found - continue on and it will give the not found error
                }

                // Path is good or couldn't be resolved
                return originalRequire.call(moduleName);
            }
        });
    }

    public LuaValue execute(String luaCode) {
        return executeWithMonitoring(() -> globals.load(luaCode).call(), "Lua code execution");
    }

    public LuaValue executeFile(String filePath) {
        // Validate that the file path is within allowed directories
        if (!isPathAllowed(filePath)) {
            throw new SecurityException("Access denied: Cannot execute file '" + filePath
                    + "' - not in allowed directories");
        }

        try {
            // read the file contents
            String luaCode =
                    new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)),
                            java.nio.charset.StandardCharsets.UTF_8);
            // load and run the lua code with monitoring
            return executeWithMonitoring(() -> globals.load(luaCode, filePath).call(),
                    "Lua file execution: " + filePath);
        } catch (SecurityException e) {
            throw e;
        } catch (MemoryLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error loading Lua file '" + filePath + "': " + e.getMessage(), e);
        }
    }

    public void set(String name, LuaValue value) {
        globals.set(name, value);
    }

    public LuaValue get(String name) {
        return globals.get(name);
    }

    private void setupLoggerFunctions(Globals globals) {
        // create logger table with log functions
        LuaTable loggerTable = new LuaTable();

        loggerTable.set("debug", LuaLogFunctions.createDebugFunction());
        loggerTable.set("info", LuaLogFunctions.createInfoFunction());
        loggerTable.set("warn", LuaLogFunctions.createWarnFunction());
        loggerTable.set("error", LuaLogFunctions.createErrorFunction());
        loggerTable.set("tableToString", LuaLogFunctions.createTableToStringFunction());

        globals.set("logger", loggerTable);
    }

    private LuaValue executeWithMonitoring(Callable<LuaValue> executionTask, String context) {
        // Skip monitoring if memory limit is disabled
        if (maxMemoryBytes == -1) {
            try {
                return executionTask.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Lua execution error: " + e.getMessage(), e);
            }
        }

        long memoryBefore = getCurrentMemoryUsage();
        AtomicReference<LuaValue> result = new AtomicReference<>();
        AtomicReference<Throwable> executionException = new AtomicReference<>();
        AtomicReference<MemoryLimitExceededException> memoryException = new AtomicReference<>();
        AtomicBoolean executionComplete = new AtomicBoolean(false);

        // Create execution thread
        Thread executionThread = new Thread(() -> {
            try {
                LuaValue value = executionTask.call();
                result.set(value);
            } catch (Throwable e) {
                executionException.set(e);
            } finally {
                executionComplete.set(true);
            }
        }, "LuaSandbox-Executor");
        executionThread.setDaemon(true);
        executionThread.start();

        // Start memory monitoring thread
        Thread monitoringThread = new Thread(() -> {
            while (!executionComplete.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(MEMORY_CHECK_INTERVAL_MS);

                    if (executionComplete.get()) {
                        break;
                    }

                    long currentMemory = getCurrentMemoryUsage();
                    long memoryUsed = currentMemory - memoryBefore;

                    if (memoryUsed > maxMemoryBytes) {
                        // Interrupt the execution thread and set exception
                        executionThread.interrupt();
                        executionComplete.set(true);

                        memoryException.set(new MemoryLimitExceededException(String.format(
                                "Memory limit exceeded during %s. Used: %d bytes (%.2f MB), Limit: %d bytes (%.2f MB)",
                                context, memoryUsed, memoryUsed / (1024.0 * 1024.0), maxMemoryBytes,
                                maxMemoryBytes / (1024.0 * 1024.0))));
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "LuaSandbox-MemoryMonitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();

        try {
            // Wait for execution thread to complete
            executionThread.join();
        } catch (InterruptedException e) {
            // Main thread was interrupted - cancel execution
            executionThread.interrupt();
            monitoringThread.interrupt();
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lua execution interrupted", e);
        } finally {
            // Stop monitoring thread
            monitoringThread.interrupt();
            try {
                monitoringThread.join(1000); // Wait up to 1 second for cleanup
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Check if memory limit was exceeded
        if (memoryException.get() != null) {
            throw memoryException.get();
        }

        // Check if execution threw an exception
        if (executionException.get() != null) {
            Throwable cause = executionException.get();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Lua execution error: " + cause.getMessage(), cause);
        }

        return result.get();
    }

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    public static class MemoryLimitExceededException extends RuntimeException {
        public MemoryLimitExceededException(String message) {
            super(message);
        }
    }
}
