package redactedrice.randomizer;

import redactedrice.randomizer.wrapper.LuaSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

// Testing the sandboxing of lua files in a functional way (i.e. with real lua scripts that try
// to do things)
public class RandomizerSandboxTest {

    private LuaSandbox sandbox;
    private String testCasesPath;
    private String includetestPath;

    @BeforeEach
    public void setUp() {
        // Set up allowed paths
        includetestPath = new File("src/test/java/redactedrice/support/modules/includetest")
                .getAbsolutePath();
        testCasesPath =
                new File("src/test/java/redactedrice/support/module_test_cases").getAbsolutePath();
        // unallowed path not needed for testing

        // Create the sandbox
        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(includetestPath);
        allowedDirectories.add(testCasesPath);
        sandbox = new LuaSandbox(allowedDirectories);
    }

    @Test
    public void testLoadfileAndExecuteFromAllowedLocation() {
        // Run the script that loads and executes a file from an allowed location
        String testFile = new File(testCasesPath, "test_loadfile_allowed.lua").getAbsolutePath();

        // Ensure it returned the expected result
        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);
        assertTrue(result.tojstring().contains("load/execute allowed"));
    }

    @Test
    public void testLoadfileAndExecuteFromUnallowedLocation() {
        // Run the script that loads and executes a file from an unallowed location
        // THis should throw an exception
        String testFile = new File(testCasesPath, "test_loadfile_unallowed.lua").getAbsolutePath();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sandbox.executeFile(testFile);
        });

        // Any of these exceptions are acceptable and may change depending on implementation
        assertTrue(exception.getMessage().contains("Access denied")
                || exception.getMessage().contains("not in allowed directories"));
    }

    @Test
    public void testRequireFromAllowedLocation() {
        // Run the script that requires a module from an allowed location
        String testFile = new File(testCasesPath, "test_require_allowed.lua").getAbsolutePath();

        // Ensure it returned the expected result
        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);
        assertTrue(result.tojstring().contains("require allowed"));
    }

    @Test
    public void testRequireFromUnallowedLocation() {
        // Test that require from unallowed location is blocked
        // The test script tries to modify package.path to include an unallowed path
        // There isn't really another way in lua to require so this is sufficient
        String testFile = new File(testCasesPath, "test_require_unallowed.lua").getAbsolutePath();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sandbox.executeFile(testFile);
        });

        // Any of these exceptions are acceptable and may change depending on implementation
        assertTrue(
                exception.getMessage().contains("Access denied")
                        || exception.getMessage().contains("not in allowed directories")
                        || exception.getMessage().contains("module 'module_fail_cases' not found"),
                "Expected access denied even after package.path modification, but got: "
                        + exception.getMessage());
    }

    @Test
    public void testMemoryLimitEnforced() {
        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(testCasesPath);
        allowedDirectories.add(includetestPath);
        LuaSandbox limitedSandbox = new LuaSandbox(allowedDirectories); // Default 100MB

        // Script will create 200 MB worth of data
        String testFile = new File(testCasesPath, "test_too_much_memory.lua").getAbsolutePath();

        // This should throw MemoryLimitExceededException
        // There is a possibility that a script could still succeed if it finishes before
        // the memory limit check is done but should not happen with this script since
        // allocates significantly more than the limit
        LuaSandbox.MemoryLimitExceededException exception =
                assertThrows(LuaSandbox.MemoryLimitExceededException.class,
                        () -> limitedSandbox.executeFile(testFile));

        assertTrue(exception.getMessage().contains("Memory limit exceeded"));
        assertTrue(exception.getMessage().contains("100.00 MB")
                || exception.getMessage().contains("104857600")); // 100 MB in bytes
    }

    @Test
    public void testNormalExecutionWithMemoryLimit() {
        // Test that normal execution works fine with memory limits
        String testFile = new File(testCasesPath, "test_loadfile_allowed.lua").getAbsolutePath();

        // Normal script should execute fine
        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);
        assertTrue(result.tojstring().contains("load/execute allowed"));

    }

    @Test
    public void testMemoryLimitDisabled() {
        // Test that memory limit can be disabled with -1
        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(testCasesPath);
        allowedDirectories.add(includetestPath);
        LuaSandbox unlimitedSandbox = new LuaSandbox(allowedDirectories, -1);

        assertEquals(-1, unlimitedSandbox.getMaxMemoryBytes());

        // Should execute normally this time since limit is disabled
        String testFile = new File(testCasesPath, "test_too_much_memory.lua").getAbsolutePath();
        LuaValue result = unlimitedSandbox.executeFile(testFile);
        assertNotNull(result);
    }

    @Test
    public void testTimeoutEnforced() throws InterruptedException {
        // Test that execution timeout is enforced using default timeout (5 seconds)
        // Run in separate thread so we can kill it if it doesn't timeout
        String testFile = new File(testCasesPath, "test_infinite_loop.lua").getAbsolutePath();

        // Run the infinite loop in a separate thread
        AtomicReference<LuaSandbox.TimeoutException> caughtException = new AtomicReference<>();
        AtomicBoolean executionCompleted = new AtomicBoolean(false);
        Thread executionThread = new Thread(() -> {
            try {
                sandbox.executeFile(testFile);
                executionCompleted.set(true);
            } catch (LuaSandbox.TimeoutException e) {
                caughtException.set(e);
                executionCompleted.set(true);
            } catch (RuntimeException e) {
                // Other exceptions are unexpected but we'll catch them
                executionCompleted.set(true);
            }
        });
        executionThread.setDaemon(true);
        executionThread.start();

        // Wait up to 10 seconds
        executionThread.join(10000);

        // Verify the execution completed
        assertTrue(executionCompleted.get() || !executionThread.isAlive());

        // If thread is still alive the timeout failed
        if (executionThread.isAlive()) {
            executionThread.interrupt();
            executionThread.join(10000);
            fail("Timeout mechanism failed - thread was still running after 10 seconds");
        }

        // Verify we got the exception
        assertNotNull(caughtException.get());
        assertTrue(caughtException.get().getMessage().contains("Execution timeout exceeded"));
        assertTrue(caughtException.get().getMessage().contains("5000") // 5 seconds in ms
                || caughtException.get().getMessage().contains("5"));
    }


    @Test
    public void testTimeoutDisabledWithInfiniteLoop() throws InterruptedException {
        // Test that timeout disabled allows infinite loop to run without timing out
        // Run it for 10 seconds to show its going well beyond the default 5 seconds
        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(testCasesPath);
        allowedDirectories.add(includetestPath);
        // No memory constraints, no timeout
        LuaSandbox noTimeoutSandbox = new LuaSandbox(allowedDirectories, -1, -1);

        assertEquals(-1, noTimeoutSandbox.getMaxExecutionTimeMs());

        String testFile = new File(testCasesPath, "test_infinite_loop.lua").getAbsolutePath();

        // Run the infinite loop in a separate thread so we can kill it after 10 seconds
        Thread executionThread = new Thread(() -> {
            try {
                noTimeoutSandbox.executeFile(testFile);
            } catch (RuntimeException e) {
                // Expected when we interrupt
            }
        });
        executionThread.setDaemon(true);
        executionThread.start();

        // Wait up to 10 seconds
        Thread.sleep(10000);

        // Thread should still be running
        assertTrue(executionThread.isAlive());

        // Now kill it manually to clean up
        executionThread.interrupt();
        executionThread.join(2000);
    }

    @Test
    public void testRequireBlockedModule() {
        // Test requiring blocked modules
        String testFile = new File(testCasesPath, "test_require_blocked_io.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("require successfully blocked IO"));
    }

    @Test
    public void testPackageLoadedInjectionBlocked() {
        // Test injected loaded packages
        String testFile =
                new File(testCasesPath, "test_package_loaded_inject_io.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("loaded successfully blocked IO injection"));
    }

    @Test
    public void testPackageLoadedDirectAccessBlocked() {
        // Test directly adding loaded packages
        String testFile =
                new File(testCasesPath, "test_package_loaded_direct_access.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("loaded successfully blocked IO"));
    }

    @Test
    public void testPackageSystemModificationBlocked() {
        // Test adding loader is blocked
        String testFile =
                new File(testCasesPath, "test_package_loaders_modification.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("loaders successfully blocked modification"));
    }

    @Test
    public void testPackagePreloadModificationBlocked() {
        // Test preloading is blocked correctly as well
        String testFile =
                new File(testCasesPath, "test_package_preload_modification.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("preload successfully blocked IO injection"));
        assertFalse(resultStr.contains("preload failed to block IO injection"));
    }

    @Test
    public void testMetatableBlocked() {
        // Test that getmetatable and setmetatable work for user tables,
        // but globals metatable is protected
        String testFile = new File(testCasesPath, "test_metatable_blocked.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("getmetatable works"));
        assertTrue(resultStr.contains("setmetatable works"));
        assertTrue(resultStr.contains("globals metatable protected"));
        assertTrue(resultStr.contains("globals metatable removal blocked"));
    }

    @Test
    public void testGlobalsProtected() {
        // Test that new global variables cannot be created but existing globals are accessible

        // Set test globals before running the script to verify globals are accessible
        sandbox.set("testGlobal", LuaValue.valueOf("test_value_123"));
        sandbox.set("testGlobalNumber", LuaValue.valueOf(42));

        String testFile = new File(testCasesPath, "test_globals_protected.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("new globals blocked"));
        assertTrue(resultStr.contains("testGlobal accessible"));
        assertTrue(resultStr.contains("testGlobalNumber accessible"));
    }

    @Test
    public void testCollectGarbageRemoved() {
        // Test that collectgarbage is blocked to prevent DoS attacks
        String testFile = new File(testCasesPath, "test_collect_garbage.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        assertNotNull(result);

        String resultStr = result.tojstring();
        assertTrue(resultStr.contains("collect garbage successfully blocked"));
    }

    @Test
    public void testPathTraversalDotDot() {
        // Test that path traversal using .. is blocked if it goes outside allowed directories
        String testFile =
                new File(testCasesPath, "test_path_traversal_dotdot.lua").getAbsolutePath();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sandbox.executeFile(testFile);
        });
        assertTrue(exception.getMessage().contains("Access denied")
                || exception.getMessage().contains("not in allowed directories")
                || exception.getMessage().contains("loadfile successfully blocked load"));
    }

    @Test
    public void testSymlinkFromUnallowedPath() throws Exception {
        // Test that symlinks pointing to files outside allowed directories are blocked
        // Git wasn't working well with symlinks so we create and remove it as part of
        // this test instead
        Path symlinkPath = Paths.get(testCasesPath).resolve("symlink_to_unallowed.lua");
        Path targetPath =
                Paths.get(testCasesPath).resolve("../module_fail_cases/unallowed_path.lua");

        try {
            // Create symlink pointing to unallowed path
            Files.createSymbolicLink(symlinkPath, targetPath);

            // Try to execute the symlink
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                sandbox.executeFile(symlinkPath.toAbsolutePath().toString());
            });

            assertTrue(exception.getMessage().contains("Access denied")
                    || exception.getMessage().contains("not in allowed directories"));
        } finally {
            // Always clean up symlink
            if (Files.exists(symlinkPath)) {
                Files.delete(symlinkPath);
            }
        }
    }

    @Test
    public void testRawsetBlocked() {
        // Test that rawset is blocked and cannot bypass protections
        String testFile = new File(testCasesPath, "test_rawset_blocked.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        String resultStr = result.tojstring();

        assertTrue(resultStr.contains("rawset successfully blocked"),
                "rawset should be blocked, got: " + resultStr);
    }

    @Test
    public void testRawgetBlocked() {
        // Test that rawget is blocked too
        String testFile = new File(testCasesPath, "test_rawget_blocked.lua").getAbsolutePath();

        LuaValue result = sandbox.executeFile(testFile);
        String resultStr = result.tojstring();

        assertTrue(resultStr.contains("rawget successfully blocked"),
                "rawget should be blocked, got: " + resultStr);
    }
}
