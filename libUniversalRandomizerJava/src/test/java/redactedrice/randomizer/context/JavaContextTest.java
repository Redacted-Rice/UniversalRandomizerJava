package redactedrice.randomizer.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JavaContextTest {

    private JavaContext context;

    @BeforeEach
    public void setUp() {
        context = new JavaContext();
    }

    @Test
    public void testRegisterObject() {
        String testObject = "test";
        context.register("obj", testObject);
        assertTrue(context.contains("obj"));
        assertEquals(testObject, context.get("obj"));
    }

    @Test
    public void testRemoveObject() {
        String testObject = "test";
        context.register("obj", testObject);
        Object removed = context.remove("obj");
        assertEquals(testObject, removed);
        assertFalse(context.contains("obj"));
    }

    @Test
    public void testRegisterEnum() {
        enum TestEnum {
            VALUE1, VALUE2
        }

        context.registerEnum("TestEnum", TestEnum.class);
        EnumRegistry enumContext = context.getEnumRegistry();
        assertTrue(enumContext.hasEnum("TestEnum"));
    }

    @Test
    public void testRegisterEnumWithCustomName() {
        enum TestEnum {
            VALUE1, VALUE2
        }

        context.registerEnum("CustomName", TestEnum.class);
        EnumRegistry enumContext = context.getEnumRegistry();
        assertTrue(enumContext.hasEnum("CustomName"));
    }

    @Test
    public void testRegisterCustomEnum() {
        context.registerEnum("Difficulty", "EASY", "NORMAL", "HARD");
        EnumRegistry enumContext = context.getEnumRegistry();
        assertTrue(enumContext.hasEnum("Difficulty"));
    }

    @Test
    public void testMergeEnumRegistry() {
        EnumRegistry source = new EnumRegistry();
        source.registerEnum("Enum1", Arrays.asList("A", "B"));

        context.mergeEnumRegistry(source);
        EnumRegistry enumContext = context.getEnumRegistry();
        assertTrue(enumContext.hasEnum("Enum1"));
    }

    @Test
    public void testToLuaTable() {
        context.register("test", "value");
        context.registerEnum("Difficulty", "EASY", "NORMAL");

        LuaTable table = context.toLuaTable();
        assertNotNull(table);
        assertEquals("value", table.get("test").tojstring());
        // enums should be tables
        assertTrue(table.get("Difficulty").istable());
    }

    @Test
    public void testExecutionModuleInToLuaTable() {
        context.setExecutionModuleName("shuffle_hp");

        LuaTable table = context.toLuaTable();
        assertEquals("shuffle_hp", table.get("executionModule").tojstring());

        context.clearExecutionModuleName();
        table = context.toLuaTable();
        assertTrue(table.get("executionModule").isnil());
    }

    @Test
    public void testSize() {
        assertEquals(0, context.size());
        context.register("obj1", "value1");
        assertEquals(1, context.size());
        context.register("obj2", "value2");
        assertEquals(2, context.size());
    }

    @Test
    public void testGetRegisteredNames() {
        context.register("obj1", "value1");
        context.register("obj2", "value2");

        String[] names = context.getRegisteredNames();
        assertEquals(2, names.length);
        assertTrue(Arrays.asList(names).contains("obj1"));
        assertTrue(Arrays.asList(names).contains("obj2"));
    }

    @Test
    public void testClear() {
        context.register("obj", "value");
        context.clear();
        assertEquals(0, context.size());
        assertFalse(context.contains("obj"));
    }

    @Test
    public void testNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            context.register(null, "value");
        });
    }

    @Test
    public void testEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            context.register("", "value");
        });
    }
}

