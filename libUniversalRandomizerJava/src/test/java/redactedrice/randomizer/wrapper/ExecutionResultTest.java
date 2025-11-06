package redactedrice.randomizer.wrapper;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Tests for ExecutionResult class
// verifies result storage, change tracking, and formatting
public class ExecutionResultTest {

    @Test
    public void testSuccessfulExecution() {
        // test that successful execution results are stored correctly
        LuaValue result = LuaString.valueOf("success");
        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, result, null, 12345);

        assertEquals("TestModule", execResult.getModuleName());
        assertTrue(execResult.isSuccess());
        assertNull(execResult.getErrorMessage());
        assertEquals(result, execResult.getResult());
        assertEquals(12345, execResult.getSeedUsed());
        assertFalse(execResult.hasChanges());
        assertNull(execResult.getChanges());
    }

    @Test
    public void testFailedExecution() {
        ExecutionResult execResult =
                new ExecutionResult("TestModule", false, "Error message", null, null, 0);

        assertEquals("TestModule", execResult.getModuleName());
        assertFalse(execResult.isSuccess());
        assertEquals("Error message", execResult.getErrorMessage());
        assertNull(execResult.getResult());
        assertEquals(0, execResult.getSeedUsed());
        assertFalse(execResult.hasChanges());
    }

    @Test
    public void testWithChanges() {
        Map<String, Map<String, String>> changes = new HashMap<>();
        Map<String, String> entityChanges = new HashMap<>();
        entityChanges.put("health", "100 -> 150");
        entityChanges.put("name", "Old -> New");
        changes.put("entity1", entityChanges);

        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, changes, 54321);

        assertTrue(execResult.hasChanges());
        Map<String, Map<String, String>> retrievedChanges = execResult.getChanges();
        assertNotNull(retrievedChanges);
        assertEquals(1, retrievedChanges.size());
        assertTrue(retrievedChanges.containsKey("entity1"));
        assertEquals(2, retrievedChanges.get("entity1").size());
    }

    @Test
    public void testGetChangesReturnsCopy() {
        Map<String, Map<String, String>> changes = new HashMap<>();
        Map<String, String> entityChanges = new HashMap<>();
        entityChanges.put("health", "100 -> 150");
        changes.put("entity1", entityChanges);

        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, changes, 123);

        Map<String, Map<String, String>> retrievedChanges1 = execResult.getChanges();
        Map<String, Map<String, String>> retrievedChanges2 = execResult.getChanges();

        // Should return different instances (defensive copy)
        assertNotSame(retrievedChanges1, retrievedChanges2);
        assertEquals(retrievedChanges1, retrievedChanges2);

        // Modifying retrieved changes shouldn't affect original
        retrievedChanges1.put("newEntity", new HashMap<>());
        Map<String, Map<String, String>> retrievedChanges3 = execResult.getChanges();
        assertFalse(retrievedChanges3.containsKey("newEntity"));
    }

    @Test
    public void testGetChangesFormatted() {
        Map<String, Map<String, String>> changes = new HashMap<>();
        Map<String, String> entityChanges1 = new HashMap<>();
        entityChanges1.put("health", "100 -> 150");
        entityChanges1.put("name", "Old -> New");
        changes.put("entity1", entityChanges1);

        Map<String, String> entityChanges2 = new HashMap<>();
        entityChanges2.put("speed", "5 -> 10");
        changes.put("entity2", entityChanges2);

        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, changes, 123);

        String formatted = execResult.getChangesFormatted();
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("entity1"));
        assertTrue(formatted.contains("entity2"));
        assertTrue(formatted.contains("health"));
        assertTrue(formatted.contains("name"));
        assertTrue(formatted.contains("speed"));
    }

    @Test
    public void testGetChangesFormattedNoChanges() {
        ExecutionResult execResult = new ExecutionResult("TestModule", true, null, null, null, 123);

        String formatted = execResult.getChangesFormatted();
        assertEquals("No changes detected", formatted);
    }

    @Test
    public void testGetChangesFormattedEmptyChanges() {
        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, new HashMap<>(), 123);

        String formatted = execResult.getChangesFormatted();
        assertEquals("No changes detected", formatted);
    }

    @Test
    public void testToStringSuccessful() {
        Map<String, Map<String, String>> changes = new HashMap<>();
        Map<String, String> entityChanges = new HashMap<>();
        entityChanges.put("health", "100 -> 150");
        changes.put("entity1", entityChanges);

        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, changes, 12345);

        String str = execResult.toString();
        assertTrue(str.contains("TestModule"));
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("seed=12345"));
        assertTrue(str.contains("changes="));
        assertTrue(str.contains("entity1"));
    }

    @Test
    public void testToStringSuccessfulNoChanges() {
        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, null, 12345);

        String str = execResult.toString();
        assertTrue(str.contains("TestModule"));
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("seed=12345"));
        assertFalse(str.contains("changes="));
    }

    @Test
    public void testToStringFailed() {
        ExecutionResult execResult =
                new ExecutionResult("TestModule", false, "Test error", null, null, 0);

        String str = execResult.toString();
        assertTrue(str.contains("TestModule"));
        assertTrue(str.contains("success=false"));
        assertTrue(str.contains("error='Test error'"));
    }

    @Test
    public void testWithLuaResult() {
        LuaInteger result = LuaInteger.valueOf(42);
        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, result, null, 999);

        assertEquals(result, execResult.getResult());
        assertEquals(42, execResult.getResult().toint());
    }

    @Test
    public void testMultipleChangesFormatted() {
        Map<String, Map<String, String>> changes = new HashMap<>();
        Map<String, String> entityChanges = new HashMap<>();
        entityChanges.put("field1", "val1 -> val2");
        entityChanges.put("field2", "val3 -> val4");
        entityChanges.put("field3", "val5 -> val6");
        changes.put("entity1", entityChanges);

        ExecutionResult execResult =
                new ExecutionResult("TestModule", true, null, null, changes, 123);

        String formatted = execResult.getChangesFormatted();
        assertTrue(formatted.contains("entity1"));
        assertTrue(formatted.contains("field1"));
        assertTrue(formatted.contains("field2"));
        assertTrue(formatted.contains("field3"));
        assertTrue(formatted.contains("val1 -> val2"));
        // Should have commas between fields
        assertTrue(formatted.contains(","));
    }
}

