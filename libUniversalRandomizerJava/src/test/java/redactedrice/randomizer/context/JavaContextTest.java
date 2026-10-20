package redactedrice.randomizer.context;

import redactedrice.randomizer.context.testsupport.ContextTestEnum;
import redactedrice.randomizer.context.testsupport.StaticMethodsTestEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JavaContextTest {

    private JavaContext context;

    @BeforeEach
    public void setUp() {
        context = new JavaContext();
    }

    @Test
    public void testRegisterAndRemoveObject() {
        String testObject = "test";
        context.register("obj", testObject);
        assertTrue(context.contains("obj"));
        assertEquals(testObject, context.get("obj"));

        Object removed = context.remove("obj");
        assertEquals(testObject, removed);
        assertFalse(context.contains("obj"));
    }

    @Test
    public void testRegisterEnumVariants() {

        context.registerEnum("ContextTestEnum", ContextTestEnum.class);
        EnumRegistry enumContext = context.getEnumRegistry();
        assertTrue(enumContext.hasEnum("ContextTestEnum"));

        context.registerEnum("CustomName", ContextTestEnum.class);
        assertTrue(enumContext.hasEnum("CustomName"));

        context.registerEnum("Difficulty", "EASY", "NORMAL", "HARD");
        assertTrue(enumContext.hasEnum("Difficulty"));
    }

    @Test
    public void testMergeEnumRegistry() {
        EnumRegistry source = new EnumRegistry();
        source.registerEnum("Enum1", Arrays.asList("A", "B"));

        context.mergeEnumRegistry(source);
        assertTrue(context.getEnumRegistry().hasEnum("Enum1"));
    }

    @Test
    public void testRegisterDynamicFieldRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class,
                () -> context.registerDynamicField(null, "Rank"));
        assertThrows(IllegalArgumentException.class,
                () -> context.registerDynamicField("", "Rank"));
        assertThrows(IllegalArgumentException.class,
                () -> context.registerDynamicField("maxStage", ""));
    }

    @Test
    public void testToLuaTable() {
        context.register("test", "value");
        context.registerEnum("Difficulty", "EASY", "NORMAL");

        LuaTable table = context.toLuaTable();
        assertNotNull(table);
        assertEquals("value", table.get("test").tojstring());
        assertTrue(table.get("Difficulty").istable());
    }

    @Test
    public void testExecutionModuleInToLuaTable() {
        context.setExecutionModuleName("example_module");

        LuaTable table = context.toLuaTable();
        assertEquals("example_module", table.get("executionModule").tojstring());

        context.clearExecutionModuleName();
        table = context.toLuaTable();
        assertTrue(table.get("executionModule").isnil());
    }

    @Test
    public void testSizeClearAndRegisteredNames() {
        assertEquals(0, context.size());
        context.register("obj1", "value1");
        context.register("obj2", "value2");
        assertEquals(2, context.size());

        String[] names = context.getRegisteredNames();
        assertEquals(2, names.length);
        assertTrue(Arrays.asList(names).contains("obj1"));
        assertTrue(Arrays.asList(names).contains("obj2"));

        context.clear();
        assertEquals(0, context.size());
        assertFalse(context.contains("obj1"));
    }

    @Test
    public void testRegisterRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> context.register(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> context.register("", "value"));
    }

    @Test
    public void testToLuaTableJavaBackedEnumTable() {
        context.registerEnum(StaticMethodsTestEnum.class);

        LuaTable enumTable = context.toLuaTable().get("StaticMethodsTestEnum").checktable();
        assertSame(StaticMethodsTestEnum.A, enumTable.get("A").touserdata());
        assertTrue(enumTable.get("values").istable());
        assertEquals(0, enumTable.get("values").checktable().get("A").toint());
        assertTrue(enumTable.get("join").isfunction());
        assertEquals("A+B",
                enumTable.get("join").call(LuaValue.valueOf("A"), LuaValue.valueOf("B")).tojstring());
        assertThrows(LuaError.class, () -> enumTable.set("extra", LuaValue.valueOf("x")));
    }

    @Test
    public void testToLuaTableLuaOnlyEnumHasNoStaticMethods() {
        context.registerEnum("Difficulty", "EASY", "NORMAL", "HARD");

        LuaTable enumTable = context.toLuaTable().get("Difficulty").checktable();
        assertEquals("EASY", enumTable.get(1).tojstring());
        assertTrue(enumTable.get("join").isnil());
    }

    @Test
    public void testRegisterJavaClassExposesStaticMethods() {
        context.register("StaticMethodsTestEnum", StaticMethodsTestEnum.class);

        LuaValue classOnly = context.toLuaTable().get("StaticMethodsTestEnum");
        assertEquals("B+A",
                classOnly.get("join").call(LuaValue.valueOf("B"), LuaValue.valueOf("A")).tojstring());

        context.registerEnum("AliasRarity", StaticMethodsTestEnum.class);
        assertEquals("A+B",
                classOnly.get("join").call(LuaValue.valueOf("A"), LuaValue.valueOf("B")).tojstring());
    }

    @Test
    public void testExtendEnumKeepsJavaEnumStaticMethods() {
        context.registerEnum(StaticMethodsTestEnum.class);

        LuaTable contextTable = context.toLuaTable();
        LuaTable extra = new LuaTable();
        extra.set("EXTRA", LuaValue.valueOf(2));

        contextTable.get("extendEnum").call(LuaValue.valueOf("StaticMethodsTestEnum"), extra);

        LuaTable enumTable = contextTable.get("StaticMethodsTestEnum").checktable();
        assertEquals("EXTRA", enumTable.get("EXTRA").tojstring());
        assertEquals("A+B",
                enumTable.get("join").call(LuaValue.valueOf("A"), LuaValue.valueOf("B")).tojstring());
    }
}
