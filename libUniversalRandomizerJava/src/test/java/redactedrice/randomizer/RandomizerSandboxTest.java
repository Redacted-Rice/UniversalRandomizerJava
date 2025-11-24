package redactedrice.randomizer;

import redactedrice.randomizer.wrapper.LuaSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
}
