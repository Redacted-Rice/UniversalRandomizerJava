package redactedrice.randomizer.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;
import redactedrice.randomizer.lua.arguments.TypeDefinition;
import redactedrice.randomizer.lua.dynamicVar.DynamicVar;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ModuleTest {

    private static final String TEST_ID = "test_module";
    private static final String TEST_NAME = "Test Module";

    private LuaFunction createMockFunction() {
        return new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
    }

    private Set<String> setOf(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private Module buildModule(String id, String name, String description, Set<String> groups,
            Set<String> modifies, List<ArgumentDefinition> arguments, LuaFunction executeFunc,
            LuaFunction onLoadFunc, String filePath, int seedOffset, boolean seedOffsetFromMetadata,
            boolean seeded, String when, String author, String version,
            Map<String, String> requires, List<DynamicVar> provides, List<DynamicVar> needs,
            String source, String license, String about) {
        return new Module(id, name, description, groups, modifies, arguments, executeFunc,
                onLoadFunc, filePath, seedOffset, seedOffsetFromMetadata, seeded, when, author,
                version, requires, provides, needs, source, license, about);
    }

    private Module buildDefaultModule() {
        return buildModule(TEST_ID, TEST_NAME, "Test description", setOf("gameplay"),
                setOf("stats", "appearance"),
                Arrays.asList(new ArgumentDefinition("arg1", TypeDefinition.string(), null)),
                createMockFunction(), null, "/path/to/module.lua", 0, true, true, null,
                "TestAuthor", "0.1", Map.of("other_module", "0.1"), null, null, null, null, null);
    }

    @Test
    public void testConstructor() {
        Module metadata = buildDefaultModule();

        assertEquals(TEST_ID, metadata.getId());
        assertEquals(TEST_NAME, metadata.getName());
        assertEquals("Test description", metadata.getDescription());
        assertEquals(1, metadata.getGroups().size());
        assertTrue(metadata.getGroups().contains("gameplay"));
        assertEquals(2, metadata.getModifies().size());
        assertTrue(metadata.getModifies().contains("stats"));
        assertTrue(metadata.getModifies().contains("appearance"));
        assertEquals(1, metadata.getArguments().size());
        assertFalse(metadata.hasOnLoad());
        assertEquals("/path/to/module.lua", metadata.getFilePath());
        assertTrue(metadata.isSeedOffsetFromMetadata());
        assertEquals(0, metadata.getSeedOffset());
        assertFalse(metadata.isScript());
        assertEquals("TestAuthor", metadata.getAuthor());
        assertEquals("0.1", metadata.getVersion());
        assertEquals("0.1", metadata.getRequires().get("other_module"));
    }

    @Test
    public void testConstructorRejectsInvalidArguments() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(null, TEST_NAME, null, null, null, null, executeFunc, null, null,
                        0, true, true, "module", "TestAuthor", "0.1", null, null, null, null, null,
                        null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule("", TEST_NAME, null, null, null, null, executeFunc, null, null, 0,
                        true, true, null, "TestAuthor", "0.1", null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule("   ", TEST_NAME, null, null, null, null, executeFunc, null, null,
                        0, true, true, null, "TestAuthor", "0.1", null, null, null, null, null,
                        null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, null, null, null, null, null, executeFunc, null, null, 0,
                        true, true, null, "TestAuthor", "0.1", null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, "", null, null, null, null, executeFunc, null, null, 0,
                        true, true, null, "TestAuthor", "0.1", null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, TEST_NAME, null, null, null, null, null, null, null, 0,
                        true, true, null, "TestAuthor", "0.1", null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                        null, 0, true, true, null, null, "0.1", null, null, null, null, null,
                        null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                        null, 0, true, true, null, "", "0.1", null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                        null, 0, true, true, null, "TestAuthor", null, null, null, null, null, null,
                        null));
    }

    @Test
    public void testRequiresWithMultipleEntries() {
        LuaFunction executeFunc = createMockFunction();
        Map<String, String> requires = new HashMap<>();
        requires.put("prescript_a", "0.1");
        requires.put("prescript_b", "0.2");
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                null, 0, true, true, "module", "TestAuthor", "0.1", requires, null, null, null,
                null, null);

        assertEquals("0.1", metadata.getRequires().get("prescript_a"));
        assertEquals("0.2", metadata.getRequires().get("prescript_b"));
    }

    @Test
    public void testConstructorAllowsEmptyRequires() {
        LuaFunction executeFunc = createMockFunction();
        Module nullRequires = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc,
                null, null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null, null,
                null, null);
        Module emptyRequires = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc,
                null, null, 0, true, true, "module", "TestAuthor", "0.1", new HashMap<>(), null,
                null, null, null, null);

        assertTrue(nullRequires.getRequires().isEmpty());
        assertTrue(emptyRequires.getRequires().isEmpty());
    }

    @Test
    public void testGettersReturnUnmodifiableCollections() {
        LuaFunction executeFunc = createMockFunction();
        Map<String, String> requires = Map.of("other_module", "0.1");
        Set<String> groups = setOf("test");
        Set<String> modifies = setOf("stats", "appearance");
        ArgumentDefinition arg1 = new ArgumentDefinition("arg1", TypeDefinition.string(), null);
        List<ArgumentDefinition> arguments = Arrays.asList(arg1);

        Module metadata = buildModule(TEST_ID, TEST_NAME, null, groups, modifies, arguments,
                executeFunc, null, null, 0, true, true, null, "TestAuthor", "0.1", requires, null,
                null, null, null, null);

        assertThrows(UnsupportedOperationException.class,
                () -> metadata.getRequires().put("NewProgram", "1.0.0"));
        assertThrows(UnsupportedOperationException.class, () -> metadata.getModifies().add("new"));
        assertThrows(UnsupportedOperationException.class, () -> metadata.getArguments()
                .add(new ArgumentDefinition("arg2", TypeDefinition.integer(), null)));
    }

    @Test
    public void testConstructorNullDescription() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null, null, null,
                null);

        assertEquals("", metadata.getDescription());
    }

    @Test
    public void testConstructorNullGroupsForScriptsAllowed() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null, null, null,
                null);

        assertTrue(metadata.getGroups().isEmpty());
    }

    @Test
    public void testConstructorWithGroupsForScriptsThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            buildModule(TEST_ID, TEST_NAME, null, setOf("test"), null, null, executeFunc, null,
                    null, 0, true, false, "module", "TestAuthor", "0.1", null, null, null, null,
                    null, null);
        });
    }

    @Test
    public void testConstructorRejectsMissingGroupsForRegularModules() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                        null, 0, true, true, null, "TestAuthor", "0.1", null, null, null, null,
                        null, null));
        assertThrows(IllegalArgumentException.class,
                () -> buildModule(TEST_ID, TEST_NAME, null, setOf(), null, null, executeFunc, null,
                        null, 0, true, true, null, "TestAuthor", "0.1", null, null, null, null,
                        null, null));
    }

    @Test
    public void testConstructorGroupsCaseInsensitive() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, setOf("GAMEPLAY"), null, null,
                executeFunc, null, null, 0, true, true, null, "TestAuthor", "0.1", null, null, null,
                null, null, null);

        assertTrue(metadata.getGroups().contains("gameplay"));
    }

    @Test
    public void testConstructorMultipleGroups() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, setOf("gameplay", "ACTION"), null,
                null, executeFunc, null, null, 0, true, true, null, "TestAuthor", "0.1", null, null,
                null, null, null, null);

        assertEquals(2, metadata.getGroups().size());
        assertTrue(metadata.getGroups().contains("gameplay"));
        assertTrue(metadata.getGroups().contains("action"));
    }

    @Test
    public void testConstructorNullModifies() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, setOf("test"), null, null,
                executeFunc, null, null, 0, true, true, null, "TestAuthor", "0.1", null, null, null,
                null, null, null);

        assertTrue(metadata.getModifies().isEmpty());
    }

    @Test
    public void testConstructorNullArguments() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null, null, null,
                null);

        assertTrue(metadata.getArguments().isEmpty());
    }

    @Test
    public void testWithOnLoadFunction() {
        LuaFunction executeFunc = createMockFunction();
        LuaFunction onLoadFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc,
                onLoadFunc, null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null,
                null, null, null);

        assertEquals(onLoadFunc, metadata.getOnLoadFunction());
        assertTrue(metadata.hasOnLoad());
    }

    @Test
    public void testOptionalMetadataFields() {
        LuaFunction executeFunc = createMockFunction();
        Module populated = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc,
                null, null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null,
                "https://github.com/example/module", "MIT", "This is a test module");
        assertEquals("https://github.com/example/module", populated.getSource());
        assertEquals("MIT", populated.getLicense());
        assertEquals("This is a test module", populated.getAbout());

        Module empty = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                null, 0, true, true, "module", "TestAuthor", "0.1", null, null, null, null, null,
                null);
        assertNull(empty.getSource());
        assertNull(empty.getLicense());
        assertNull(empty.getAbout());
    }

    @Test
    public void testToString() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata =
                buildModule(TEST_ID, TEST_NAME, "Description", setOf("gameplay"), setOf("stats"),
                        Arrays.asList(
                                new ArgumentDefinition("arg1", TypeDefinition.string(), null)),
                        executeFunc, null, "/path/to/module.lua", 5, true, true, null, "TestAuthor",
                        "0.1", null, null, null, null, null, null);

        String str = metadata.toString();
        assertTrue(str.contains("Module"));
        assertTrue(str.contains("id='test_module'"));
        assertTrue(str.contains("name='Test Module'"));
        assertTrue(str.contains("groups=[gameplay]"));
        assertTrue(str.contains("description='Description'"));
        assertTrue(str.contains("arguments=1"));
        assertTrue(str.contains("seedOffset=5"));
        assertTrue(str.contains("author='TestAuthor'"));
        assertTrue(str.contains("version='0.1'"));
    }

    @Test
    public void testforModuleOffset() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                null, 42, true, true, "module", "TestAuthor", "0.1", null, null, null, null, null,
                null);

        assertTrue(metadata.isSeedOffsetFromMetadata());
        assertEquals(42, metadata.getSeedOffset());
        assertTrue(metadata.isSeeded());
    }

    @Test
    public void testSeededFalseMetadata() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule("test_script", "Test Script", null, null, null, null,
                executeFunc, null, null, 0, true, false, "randomize", "TestAuthor", "0.1", null,
                null, null, null, null, null);

        assertFalse(metadata.isSeeded());
        assertTrue(metadata.isScript());
    }

    @Test
    public void testWithFilePath() {
        LuaFunction executeFunc = createMockFunction();
        Module metadata = buildModule(TEST_ID, TEST_NAME, null, null, null, null, executeFunc, null,
                "/custom/path.lua", 0, true, true, "module", "TestAuthor", "0.1", null, null, null,
                null, null, null);

        assertEquals("/custom/path.lua", metadata.getFilePath());
    }
}
