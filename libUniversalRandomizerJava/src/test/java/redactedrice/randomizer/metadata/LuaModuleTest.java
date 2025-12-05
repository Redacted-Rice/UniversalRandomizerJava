package redactedrice.randomizer.metadata;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;
import redactedrice.randomizer.wrapper.LuaModule;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class LuaModuleTest {

    private LuaFunction createMockFunction() {
        return new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
    }

    private Map<String, String> createRequiresMap() {
        Map<String, String> requires = new HashMap<>();
        requires.put("UniversalRandomizerJava", "0.5.0");
        return requires;
    }

    private Set<String> setOf(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @Test
    public void testConstructor() {
        LuaFunction executeFunc = createMockFunction();
        Map<String, String> requires = createRequiresMap();
        LuaModule metadata = new LuaModule("TestModule", "Test description", setOf("gameplay"),
                setOf("stats", "appearance"),
                Arrays.asList(new ArgumentDefinition("arg1", TypeDefinition.string(), null)),
                executeFunc, null, "/path/to/module.lua", 0, null, "TestAuthor", "0.1", requires,
                null, null, null);

        assertEquals("TestModule", metadata.getName());
        assertEquals("Test description", metadata.getDescription());
        assertEquals(1, metadata.getGroups().size());
        assertTrue(metadata.getGroups().contains("gameplay"));
        assertEquals(2, metadata.getModifies().size());
        assertTrue(metadata.getModifies().contains("stats"));
        assertTrue(metadata.getModifies().contains("appearance"));
        assertEquals(1, metadata.getArguments().size());
        assertEquals(executeFunc, metadata.getExecuteFunction());
        assertNull(metadata.getOnLoadFunction());
        assertFalse(metadata.hasOnLoad());
        assertEquals("/path/to/module.lua", metadata.getFilePath());
        assertEquals(0, metadata.getDefaultSeedOffset());
        assertFalse(metadata.isScript());
        assertEquals("TestAuthor", metadata.getAuthor());
        assertEquals("0.1", metadata.getVersion());
        assertEquals("0.5.0", metadata.getRequires().get("UniversalRandomizerJava"));
    }

    @Test
    public void testConstructorNullNameThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule(null, null, null, null, null, executeFunc, null, null, 0, "module",
                    "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorEmptyNameThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorWhitespaceNameThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("   ", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorNullExecuteFunctionThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, null, null, null, 0, null,
                    "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorNullAuthorThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    null, "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorEmptyAuthorThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorWhitespaceAuthorThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "   ", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorNullVersionThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", null, createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorEmptyVersionThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorWhitespaceVersionThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "   ", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorNullRequiresThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", null, null, null, null);
        });
    }

    @Test
    public void testConstructorEmptyRequiresThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", new HashMap<>(), null, null, null);
        });
    }

    @Test
    public void testRequiresWithMultipleEntries() {
        LuaFunction executeFunc = createMockFunction();
        Map<String, String> requires = new HashMap<>();
        requires.put("UniversalRandomizerJava", "0.5.0");
        requires.put("TestRequired", "0.1");
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", requires, null, null, null);

        assertEquals("0.5.0", metadata.getRequires().get("UniversalRandomizerJava"));
        assertEquals("0.1", metadata.getRequires().get("TestRequired"));
    }

    @Test
    public void testConstructorRequiresMissingUniversalRandomizerJavaThrows() {
        LuaFunction executeFunc = createMockFunction();
        Map<String, String> requires = new HashMap<>();
        requires.put("TestRequired", "0.1");
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", requires, null, null, null);
        });
    }

    @Test
    public void testGetRequiresReturnsUnmodifiableMap() {
        LuaFunction executeFunc = createMockFunction();
        Map<String, String> requires = createRequiresMap();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", requires, null, null, null);

        Map<String, String> retrieved = metadata.getRequires();
        assertThrows(UnsupportedOperationException.class, () -> {
            retrieved.put("NewProgram", "1.0.0");
        });
    }

    @Test
    public void testConstructorNullDescription() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertEquals("", metadata.getDescription());
    }

    @Test
    public void testConstructorNullGroupsForScriptsAllowed() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        // Scripts should not have groups
        assertTrue(metadata.getGroups().isEmpty());
    }

    @Test
    public void testConstructorWithGroupsForScriptsThrows() {
        LuaFunction executeFunc = createMockFunction();
        // Scripts should not have groups
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, setOf("test"), null, null, executeFunc, null, null, 0,
                    "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorNullGroupsForRegularModuleThrows() {
        LuaFunction executeFunc = createMockFunction();
        // Regular modules require groups
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, null, null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorEmptyGroupsForRegularModuleThrows() {
        LuaFunction executeFunc = createMockFunction();
        // Regular modules cannot have empty groups
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModule("TestModule", null, setOf(), null, null, executeFunc, null, null, 0, null,
                    "TestAuthor", "0.1", createRequiresMap(), null, null, null);
        });
    }

    @Test
    public void testConstructorGroupsCaseInsensitive() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata =
                new LuaModule("TestModule", null, setOf("GAMEPLAY"), null, null, executeFunc, null,
                        null, 0, null, "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertTrue(metadata.getGroups().contains("gameplay")); // Lowercased
    }

    @Test
    public void testConstructorMultipleGroups() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, setOf("gameplay", "ACTION"), null,
                null, executeFunc, null, null, 0, null, "TestAuthor", "0.1", createRequiresMap(),
                null, null, null);

        assertEquals(2, metadata.getGroups().size());
        assertTrue(metadata.getGroups().contains("gameplay"));
        assertTrue(metadata.getGroups().contains("action")); // Lowercased
    }

    @Test
    public void testConstructorNullModifies() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata =
                new LuaModule("TestModule", null, setOf("test"), null, null, executeFunc, null,
                        null, 0, null, "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertTrue(metadata.getModifies().isEmpty());
    }

    @Test
    public void testConstructorNullArguments() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertTrue(metadata.getArguments().isEmpty());
    }

    @Test
    public void testWithOnLoadFunction() {
        LuaFunction executeFunc = createMockFunction();
        LuaFunction onLoadFunc = createMockFunction();
        LuaModule metadata =
                new LuaModule("TestModule", null, null, null, null, executeFunc, onLoadFunc, null,
                        0, "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertEquals(onLoadFunc, metadata.getOnLoadFunction());
        assertTrue(metadata.hasOnLoad());
    }

    @Test
    public void testGetModifiesReturnsUnmodifiableSet() {
        LuaFunction executeFunc = createMockFunction();
        Set<String> groups = setOf("test");
        Set<String> modifies = setOf("stats", "appearance");
        LuaModule metadata = new LuaModule("TestModule", null, groups, modifies, null, executeFunc,
                null, null, 0, null, "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        Set<String> retrieved = metadata.getModifies();
        assertThrows(UnsupportedOperationException.class, () -> {
            retrieved.add("new");
        });
    }

    @Test
    public void testGetArgumentsReturnsUnmodifiableList() {
        LuaFunction executeFunc = createMockFunction();
        ArgumentDefinition arg1 = new ArgumentDefinition("arg1", TypeDefinition.string(), null);
        List<ArgumentDefinition> arguments = Arrays.asList(arg1);
        LuaModule metadata =
                new LuaModule("TestModule", null, null, null, arguments, executeFunc, null, null, 0,
                        "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        List<ArgumentDefinition> retrieved = metadata.getArguments();
        assertThrows(UnsupportedOperationException.class, () -> {
            retrieved.add(new ArgumentDefinition("arg2", TypeDefinition.integer(), null));
        });
    }

    @Test
    public void testToString() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata =
                new LuaModule("TestModule", "Description", setOf("gameplay"), setOf("stats"),
                        Arrays.asList(
                                new ArgumentDefinition("arg1", TypeDefinition.string(), null)),
                        executeFunc, null, "/path/to/module.lua", 5, null, "TestAuthor", "0.1",
                        createRequiresMap(), null, null, null);

        String str = metadata.toString();
        assertTrue(str.contains("LuaModule"));
        assertTrue(str.contains("name='TestModule'"));
        assertTrue(str.contains("groups=[gameplay]"));
        assertTrue(str.contains("description='Description'"));
        assertTrue(str.contains("arguments=1"));
        assertTrue(str.contains("modifies="));
        assertTrue(str.contains("seedOffset=5"));
        assertTrue(str.contains("filePath='/path/to/module.lua'"));
        assertTrue(str.contains("author='TestAuthor'"));
        assertTrue(str.contains("version='0.1'"));
    }

    @Test
    public void testWithSeedOffset() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 42, "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertEquals(42, metadata.getDefaultSeedOffset());
    }

    @Test
    public void testWithFilePath() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                "/custom/path.lua", 0, "module", "TestAuthor", "0.1", createRequiresMap(), null,
                null, null);

        assertEquals("/custom/path.lua", metadata.getFilePath());
    }

    @Test
    public void testOptionalFieldsSourceLicenseAbout() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", createRequiresMap(),
                "https://github.com/example/module", "MIT", "This is a test module");

        assertEquals("https://github.com/example/module", metadata.getSource());
        assertEquals("MIT", metadata.getLicense());
        assertEquals("This is a test module", metadata.getAbout());
    }

    @Test
    public void testOptionalFieldsCanBeNull() {
        LuaFunction executeFunc = createMockFunction();
        LuaModule metadata = new LuaModule("TestModule", null, null, null, null, executeFunc, null,
                null, 0, "module", "TestAuthor", "0.1", createRequiresMap(), null, null, null);

        assertNull(metadata.getSource());
        assertNull(metadata.getLicense());
        assertNull(metadata.getAbout());
    }
}

