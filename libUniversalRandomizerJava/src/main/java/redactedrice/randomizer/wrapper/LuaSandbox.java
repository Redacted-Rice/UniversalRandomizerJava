package redactedrice.randomizer.wrapper;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// sandboxed lua environment that blocks dangerous functions and libraries
public class LuaSandbox {
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
    private static final boolean GC_BEFORE_SNAPSHOT = false;

    // Dangerous modules to block and not allow
    // Note Debug is handled specially as need to expose part of it in order
    // to have stack traces
    private static final Set<String> BLOCKED_MODULES =
            new HashSet<>(Arrays.asList("io", "os", "luajava"));

    Globals globals;
    List<Path> allowedRootDirectories;
    private long maxMemoryBytes;
    private long maxExecutionTimeMs;
    private long monitoringIntervalMs;
    private boolean gcBeforeSnapshot;

    public LuaSandbox(List<String> allowedRootDirectories) {
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

        this.maxMemoryBytes = DEFAULT_MAX_MEMORY_BYTES;
        this.maxExecutionTimeMs = DEFAULT_MAX_EXECUTION_TIME_MS;
        this.monitoringIntervalMs = DEFAULT_MONITORING_INTERVAL_MS;
        this.gcBeforeSnapshot = GC_BEFORE_SNAPSHOT;

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

            // Resolve symlinks if there are any
            Path resolvedPath;
            try {
                resolvedPath = requestedPath.toRealPath();
            } catch (java.nio.file.NoSuchFileException e) {
                // Use the normalized path
                resolvedPath = requestedPath;
            }

            // Check if the resolved path is within an allowed directory
            for (Path allowedRoot : allowedRootDirectories) {
                Path normalizedAllowed = allowedRoot.toAbsolutePath().normalize();
                if (resolvedPath.startsWith(normalizedAllowed)) {
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

        // protect globals from modification
        // Note this must be done last or else we will block ourself
        setupProtectedGlobals(globals);

        return globals;
    }

    private void blockDangerousFunctions(Globals globals) {
        // Remove blocked modules completely
        // Note this does NOT include debug intentionally
        for (String moduleName : BLOCKED_MODULES) {
            globals.set(moduleName, LuaValue.NIL);
        }

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

        // Remove collectgarbage. No reason to have it and it can be abused. Probably a bit overkill
        // but it was simple to add so I decided just to do it
        globals.set("collectgarbage", LuaValue.NIL);

        // Block rawset and rawget to prevent bypassing our protections
        // I am not sure if rawget is really needed but there also isn't really
        // any reason why they should be using it so I remove it to be safe
        globals.set("rawset", LuaValue.NIL);
        globals.set("rawget", LuaValue.NIL);

        // Restrict setmetatable to prevent bypassing security protections
        // We allow getmetatable as its read only
        setupRestrictedSetMetatable(globals);

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

            // Protect package loaded, searchers, and preload from manipulation
            setupRestrictedPackageLoaded(packageLib);
            setupRestrictedPackageSearchers(packageLib);
            setupRestrictedPackagePreload(packageLib);

            // Make package.path and package.cpath read only
            // This needs to be done after setting the paths above
            setupProtectedPackagePath(globals, packageLib, allowedPackagePath);
        } else {
            // this should always be set but just in case package path is excluded already
            // don't add it back in
            allowedPackagePath = "";
        }

        // wrap require to validate that resolved paths are within allowed directories
        final LuaValue originalRequire = globals.get("require");
        final LuaValue packageLibFinal = packageLib;

        globals.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue moduleName) {
                String module = moduleName.tojstring();

                // Validate module name is not blocked
                if (isBlockedModuleName(module)) {
                    throw new SecurityException(
                            "Access denied: Cannot require blocked module '" + module + "'");
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

    private boolean isBlockedModuleName(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return false;
        }

        String normalized = moduleName.trim();

        // Is the module blocked?
        if (BLOCKED_MODULES.contains(normalized)) {
            return true;
        }

        // Block debug even though it's restricted rather than completely removed
        // to prevent loading the full debug module via require
        if (normalized.equals("debug")) {
            return true;
        }

        return false;
    }

    // base class for lua table wrappers to control access/writing
    // By default it just passes everthing to the original
    private abstract class DelegatingLuaTable extends LuaTable {
        protected final LuaTable original;

        protected DelegatingLuaTable(LuaTable original) {
            this.original = original;
        }

        @Override
        public LuaValue rawget(LuaValue key) {
            return original.rawget(key);
        }

        @Override
        public LuaValue get(LuaValue key) {
            return original.get(key);
        }

        @Override
        public LuaValue get(int key) {
            return original.get(key);
        }

        @Override
        public void set(LuaValue key, LuaValue value) {
            rawset(key, value);
        }

        @Override
        public Varargs next(LuaValue key) {
            return original.next(key);
        }

        @Override
        public int length() {
            return original.length();
        }
    }

    // Creates a read only table for lua so it can't be modified to inlcude
    // unloaded or blocked packages
    private LuaTable createReadOnlyProtectedTable(LuaTable original, String tableName) {
        return new DelegatingLuaTable(original) {
            @Override
            public void rawset(LuaValue key, LuaValue value) {
                throw new SecurityException("Cannot modify package: " + tableName);
            }
        };
    }

    // Creates a table that blocks the packages we want to exclude for
    // security reasons
    private LuaTable createModuleFilteredProtectedTable(LuaTable original) {
        return new DelegatingLuaTable(original) {
            @Override
            public void rawset(LuaValue key, LuaValue value) {
                // Only allow setting if module name is not blocked
                if (key.isstring()) {
                    String moduleName = key.tojstring();
                    if (isBlockedModuleName(moduleName)) {
                        throw new SecurityException("Cannot load blocked module:" + moduleName);
                    }
                }
                original.rawset(key, value);
            }

            @Override
            public LuaValue rawget(LuaValue key) {
                // Check if trying to access a blocked module
                if (key.isstring()) {
                    String moduleName = key.tojstring();
                    if (isBlockedModuleName(moduleName)) {
                        // Return nil instead of allowing access to potentially injected modules
                        return LuaValue.NIL;
                    }
                }
                return original.rawget(key);
            }

            @Override
            public LuaValue get(LuaValue key) {
                // Use rawget to apply filtering
                return rawget(key);
            }
        };
    }

    private void setupRestrictedPackageLoaded(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        LuaValue loaded = packageLib.get("loaded");
        if (loaded.isnil() || !loaded.istable()) {
            return;
        }

        final LuaTable originalLoaded = (LuaTable) loaded;
        packageLib.set("loaded", createModuleFilteredProtectedTable(originalLoaded));
    }

    private void setupRestrictedPackageSearchers(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        // <= Lua 5.1 uses loaders, >= 5.2 uses searchers so we need to
        // block both for compatibility

        LuaValue loaders = packageLib.get("loaders");
        if (!loaders.isnil() && loaders.istable()) {
            packageLib.set("loaders", createReadOnlyProtectedTable((LuaTable) loaders, "loaders"));
        }

        LuaValue searchers = packageLib.get("searchers");
        if (!searchers.isnil() && searchers.istable()) {
            packageLib.set("searchers",
                    createReadOnlyProtectedTable((LuaTable) searchers, "searchers"));
        }
    }

    private void setupRestrictedPackagePreload(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        LuaValue preload = packageLib.get("preload");
        if (!preload.isnil() && preload.istable()) {
            packageLib.set("preload", createReadOnlyProtectedTable((LuaTable) preload, "preload"));
        }
    }

    private void setupProtectedPackagePath(Globals globals, LuaValue packageLib,
            String allowedPackagePath) {
        if (packageLib.isnil()) {
            return;
        }
        LuaTable packageTable = (LuaTable) packageLib;

        // set the packages to the allowed paths
        packageTable.rawset("path", LuaValue.valueOf(allowedPackagePath));
        // block c library loading completely
        packageTable.rawset("cpath", LuaValue.valueOf(""));

        // Create a wrapper table that blocks changes to path and cpath
        LuaTable protectedPackage = new DelegatingLuaTable(packageTable) {
            @Override
            public void rawset(LuaValue key, LuaValue value) {
                // Block changes to path and cpath fields
                if (key.isstring()) {
                    String keyStr = key.tojstring();
                    if (keyStr.equals("path") || keyStr.equals("cpath")) {
                        throw new SecurityException("Cannot modify package." + keyStr);
                    }
                }
                original.rawset(key, value);
            }

            // Also override normal set. Just delegate to rawset for simplicity
            @Override
            public void set(LuaValue key, LuaValue value) {
                rawset(key, value);
            }
        };
        globals.set("package", protectedPackage);
    }

    // Note: Use this for dynamic code generation or to bypass path checks
    // Should only be used from the Java side and not used to execute untrusted
    // lua scripts. This still does most of the other security protections.
    public LuaValue execute(String luaCode) throws TimeoutException {
        // Load and parse the code then execute so the loading is not counted against the timeout
        LuaValue chunk = globals.load(luaCode);
        return executeWithMonitoring(() -> chunk.call(), "Lua code execution");
    }

    // Note: Primiary execute for files. This will ensure they are from an
    // expected path and run with full security
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
            // Load and parse the code then execute so the loading is not counted against the
            // timeout
            LuaValue chunk = globals.load(luaCode, filePath);
            return executeWithMonitoring(() -> chunk.call(), "Lua file execution: " + filePath);
        } catch (SecurityException e) {
            throw e;
        } catch (MemoryLimitExceededException e) {
            throw e;
        } catch (TimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error loading Lua file '" + filePath + "': " + e.getMessage(), e);
        }
    }

    public void set(String name, LuaValue value) {
        // Use rawset to bypass the __newindex metamethod protection
        // This allows us to set globals without triggering the security protection meant for Lua
        // scripts
        globals.rawset(name, value);
    }

    public LuaValue get(String name) {
        return globals.get(name);
    }

    private void setupRestrictedSetMetatable(Globals globals) {
        // Block global and restricted functions metatables from being modified
        final LuaValue originalSetmetatable = globals.get("setmetatable");
        final Globals globalsFinal = globals;
        globals.set("setmetatable", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue metatable) {
                // Prevent modification of globals table metatable
                if (table == globalsFinal
                        || (table.istable() && table.touserdata() == globalsFinal)) {
                    throw new SecurityException("Cannot modify metatable of global environment");
                }
                // Prevent modification of package system tables metatables
                LuaValue packageLib = globalsFinal.get("package");
                if (!packageLib.isnil() && packageLib.istable()) {
                    LuaTable packageTable = (LuaTable) packageLib;
                    String[] protectedPackageTables = {"loaded", "loaders", "searchers", "preload"};

                    for (String tableName : protectedPackageTables) {
                        LuaValue protectedTable = packageTable.get(tableName);
                        if (!protectedTable.isnil()
                                && (table == protectedTable || table.equals(protectedTable))) {
                            throw new SecurityException(
                                    "Cannot modify metatable of protected " + tableName + " table");
                        }
                    }
                }
                // Allow setmetatable for all other tables
                return originalSetmetatable.call(table, metatable);
            }
        });
    }

    private void setupProtectedGlobals(Globals globals) {
        // Create a metatable that prevents new global assignments
        LuaTable globalsMetatable = new LuaTable();

        globalsMetatable.set(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            // __newindex is only called for keys that don't exist in the table
            // so we can just block it to prevent new assignments
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                throw new SecurityException("Cannot create new global variable '" + key.tojstring()
                        + "'. Global environment is protected. Use local variables instead.");
            }
        });
        // Don't modify __index so we can still access existing globals

        globals.setmetatable(globalsMetatable);
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

    private LuaValue executeWithMonitoring(Callable<LuaValue> executionTask, String context)
            throws TimeoutException {
        // Skip monitoring if both limits are disabled or monitoring interval is disabled
        if ((maxMemoryBytes == MAX_MEMORY_DISABLED
                && maxExecutionTimeMs == MAX_EXECUTION_TIME_DISABLED)
                || monitoringIntervalMs == MONITORING_INTERVAL_DISABLED) {
            try {
                return executionTask.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Lua execution error: " + e.getMessage(), e);
            }
        }

        // Clear up memory and wait a bit for it if set to do so
        if (gcBeforeSnapshot && maxMemoryBytes != MAX_MEMORY_DISABLED) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long memoryBefore = getCurrentMemoryUsage();
        long executionStartTime = System.currentTimeMillis();
        AtomicReference<LuaValue> result = new AtomicReference<>();
        AtomicReference<Throwable> executionException = new AtomicReference<>();
        AtomicReference<MemoryLimitExceededException> memoryException = new AtomicReference<>();
        AtomicReference<TimeoutException> timeoutException = new AtomicReference<>();
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

        // Start monitoring thread to check memory and timeout
        Thread monitoringThread = null;
        if ((maxMemoryBytes != MAX_MEMORY_DISABLED
                || maxExecutionTimeMs != MAX_EXECUTION_TIME_DISABLED)
                && monitoringIntervalMs != MONITORING_INTERVAL_DISABLED) {
            monitoringThread = new Thread(() -> {
                while (!executionComplete.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        // monitoring interval must be valid if we are here so no need
                        // to check it
                        Thread.sleep(monitoringIntervalMs);

                        if (executionComplete.get()) {
                            break;
                        }

                        // Check timeout
                        if (maxExecutionTimeMs != MAX_EXECUTION_TIME_DISABLED) {
                            long elapsedTime = System.currentTimeMillis() - executionStartTime;
                            if (elapsedTime > maxExecutionTimeMs) {
                                executionThread.interrupt();
                                executionComplete.set(true);
                                timeoutException.set(new TimeoutException(String.format(
                                        "Execution timeout exceeded during %s. Elapsed: %d ms, Limit: %d ms",
                                        context, elapsedTime, maxExecutionTimeMs)));
                                break;
                            }
                        }

                        // Compary current memory to initial memory to determine if the
                        // script exceeds its allotment. Note its possible for this to be
                        // negative if memory is freed after the initial snapshot
                        if (maxMemoryBytes != MAX_MEMORY_DISABLED) {
                            long memoryNow = getCurrentMemoryUsage();
                            long memoryUsed = memoryNow - memoryBefore;

                            if (memoryUsed > maxMemoryBytes) {
                                executionThread.interrupt();
                                executionComplete.set(true);
                                memoryException.set(new MemoryLimitExceededException(String.format(
                                        "Memory limit exceeded during %s. Used: %d bytes (%.2f MB), Limit: %d bytes (%.2f MB)",
                                        context, memoryUsed, memoryUsed / (1024.0 * 1024.0),
                                        maxMemoryBytes, maxMemoryBytes / (1024.0 * 1024.0))));
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "LuaSandbox-Monitor");
            monitoringThread.setDaemon(true);
            monitoringThread.start();
        }

        try {
            // Wait for execution thread to complete
            if (maxExecutionTimeMs != MAX_EXECUTION_TIME_DISABLED) {
                executionThread.join(maxExecutionTimeMs);
                // Check if timeout was exceeded - i.e. thread is still running
                if (executionThread.isAlive()) {
                    executionThread.interrupt();
                    executionComplete.set(true);
                    // Wait a moment for monitoring thread to set exception if it hasn't already
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // If monitoring thread didn't set timeout exception, set it here
                    if (timeoutException.get() == null) {
                        timeoutException.set(new TimeoutException(
                                String.format("Execution timeout exceeded during %s. Limit: %d ms",
                                        context, maxExecutionTimeMs)));
                    }
                }
            } else {
                executionThread.join();
            }
        } catch (InterruptedException e) {
            // Main thread was interrupted - cancel execution
            executionThread.interrupt();
            if (monitoringThread != null) {
                monitoringThread.interrupt();
            }
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lua execution interrupted", e);
        } finally {
            // Stop monitoring thread
            if (monitoringThread != null) {
                monitoringThread.interrupt();
                try {
                    monitoringThread.join(1000); // Wait up to 1 second for cleanup
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Check if timeout was exceeded
        if (timeoutException.get() != null) {
            throw timeoutException.get();
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

    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

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

    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    public boolean doGcBeforeSnapshot() {
        return gcBeforeSnapshot;
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

    public long getMonitoringIntervalMs() {
        return monitoringIntervalMs;
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

    public static class MemoryLimitExceededException extends RuntimeException {
        public MemoryLimitExceededException(String message) {
            super(message);
        }
    }

    public static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
