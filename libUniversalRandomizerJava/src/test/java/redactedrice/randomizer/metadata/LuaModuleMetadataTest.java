package redactedrice.randomizer.metadata;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LuaModuleMetadataTest {

    private LuaFunction createMockFunction() {
        return new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.NIL;
            }
        };
    }

    @Test
    public void testConstructor() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", "Test description", "gameplay", 
            Arrays.asList("stats", "appearance"), 
            Arrays.asList(new ArgumentDefinition("arg1", TypeDefinition.string(), null)),
            executeFunc, null, "/path/to/module.lua", 0
        );
        
        assertEquals("TestModule", metadata.getName());
        assertEquals("Test description", metadata.getDescription());
        assertEquals("gameplay", metadata.getGroup());
        assertEquals(2, metadata.getModifies().size());
        assertTrue(metadata.getModifies().contains("stats"));
        assertTrue(metadata.getModifies().contains("appearance"));
        assertEquals(1, metadata.getArguments().size());
        assertEquals(executeFunc, metadata.getExecuteFunction());
        assertNull(metadata.getOnLoadFunction());
        assertFalse(metadata.hasOnLoad());
        assertEquals("/path/to/module.lua", metadata.getFilePath());
        assertEquals(0, metadata.getDefaultSeedOffset());
    }

    @Test
    public void testConstructorNullNameThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModuleMetadata(null, null, null, null, null, executeFunc, null, null, 0);
        });
    }

    @Test
    public void testConstructorEmptyNameThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModuleMetadata("", null, null, null, null, executeFunc, null, null, 0);
        });
    }

    @Test
    public void testConstructorWhitespaceNameThrows() {
        LuaFunction executeFunc = createMockFunction();
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModuleMetadata("   ", null, null, null, null, executeFunc, null, null, 0);
        });
    }

    @Test
    public void testConstructorNullExecuteFunctionThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LuaModuleMetadata("TestModule", null, null, null, null, null, null, null, 0);
        });
    }

    @Test
    public void testConstructorNullDescription() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, null, null, 0
        );
        
        assertEquals("", metadata.getDescription());
    }

    @Test
    public void testConstructorNullGroup() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, null, null, 0
        );
        
        assertEquals("utility", metadata.getGroup()); // Default group
    }

    @Test
    public void testConstructorGroupCaseInsensitive() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, "GAMEPLAY", null, null, executeFunc, null, null, 0
        );
        
        assertEquals("gameplay", metadata.getGroup()); // Lowercased
    }

    @Test
    public void testConstructorNullModifies() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, null, null, 0
        );
        
        assertTrue(metadata.getModifies().isEmpty());
    }

    @Test
    public void testConstructorNullArguments() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, null, null, 0
        );
        
        assertTrue(metadata.getArguments().isEmpty());
    }

    @Test
    public void testWithOnLoadFunction() {
        LuaFunction executeFunc = createMockFunction();
        LuaFunction onLoadFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, onLoadFunc, null, 0
        );
        
        assertEquals(onLoadFunc, metadata.getOnLoadFunction());
        assertTrue(metadata.hasOnLoad());
    }

    @Test
    public void testGetModifiesReturnsUnmodifiableList() {
        LuaFunction executeFunc = createMockFunction();
        List<String> modifies = Arrays.asList("stats", "appearance");
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, modifies, null, executeFunc, null, null, 0
        );
        
        List<String> retrieved = metadata.getModifies();
        assertThrows(UnsupportedOperationException.class, () -> {
            retrieved.add("new");
        });
    }

    @Test
    public void testGetArgumentsReturnsUnmodifiableList() {
        LuaFunction executeFunc = createMockFunction();
        ArgumentDefinition arg1 = new ArgumentDefinition("arg1", TypeDefinition.string(), null);
        List<ArgumentDefinition> arguments = Arrays.asList(arg1);
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, arguments, executeFunc, null, null, 0
        );
        
        List<ArgumentDefinition> retrieved = metadata.getArguments();
        assertThrows(UnsupportedOperationException.class, () -> {
            retrieved.add(new ArgumentDefinition("arg2", TypeDefinition.integer(), null));
        });
    }

    @Test
    public void testToString() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", "Description", "gameplay", 
            Arrays.asList("stats"), 
            Arrays.asList(new ArgumentDefinition("arg1", TypeDefinition.string(), null)),
            executeFunc, null, "/path/to/module.lua", 5
        );
        
        String str = metadata.toString();
        assertTrue(str.contains("LuaModuleMetadata"));
        assertTrue(str.contains("name='TestModule'"));
        assertTrue(str.contains("group='gameplay'"));
        assertTrue(str.contains("description='Description'"));
        assertTrue(str.contains("arguments=1"));
        assertTrue(str.contains("seedOffset=5"));
        assertTrue(str.contains("filePath='/path/to/module.lua'"));
    }

    @Test
    public void testWithSeedOffset() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, null, null, 42
        );
        
        assertEquals(42, metadata.getDefaultSeedOffset());
    }

    @Test
    public void testWithFilePath() {
        LuaFunction executeFunc = createMockFunction();
        LuaModuleMetadata metadata = new LuaModuleMetadata(
            "TestModule", null, null, null, null, executeFunc, null, "/custom/path.lua", 0
        );
        
        assertEquals("/custom/path.lua", metadata.getFilePath());
    }
}

