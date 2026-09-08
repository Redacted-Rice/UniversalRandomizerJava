package redactedrice.randomizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.EnumRegistry;
import redactedrice.randomizer.context.testsupport.ContextTestEnum;
import redactedrice.support.test.EntityType;
import redactedrice.support.test.FlagEnum;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LuaRandomizerWrapperSharedEnumTest {

    private LuaRandomizerWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new LuaRandomizerWrapper(List.of("."), List.of("."));
    }

    @Test
    void registerSharedEnumFromClassUsesSimpleName() {
        wrapper.registerSharedEnum(EntityType.class);

        EnumRegistry registry = wrapper.getEnumRegistry();
        assertTrue(registry.hasEnum("EntityType"));
        assertEquals(List.of("WARRIOR", "MAGE", "ROGUE", "CLERIC", "RANGER"),
                registry.getEnum("EntityType").getValues());
    }

    @Test
    void registerSharedEnumFromClassWithDisplayNames() {
        wrapper.registerSharedEnum(FlagEnum.class, Map.of("FLAG_ONE", "Flag One"));

        EnumDefinition enumDef = wrapper.getEnumRegistry().getEnum("FlagEnum");
        assertEquals("Flag One", enumDef.getValueDisplayName("FLAG_ONE"));
    }

    @Test
    void registerSharedEnumWithCustomName() {
        wrapper.registerSharedEnum("CustomEntity", EntityType.class);

        EnumRegistry registry = wrapper.getEnumRegistry();
        assertTrue(registry.hasEnum("CustomEntity"));
        assertEquals("MAGE", registry.getEnum("CustomEntity").resolveCanonicalValue("MAGE"));
    }

    @Test
    void registerSharedEnumWithCustomNameAndDisplayNames() {
        wrapper.registerSharedEnum("Roles", EntityType.class,
                Map.of("WARRIOR", "Fighter", "MAGE", "Wizard"));

        EnumDefinition enumDef = wrapper.getEnumRegistry().getEnum("Roles");
        assertEquals("Fighter", enumDef.getValueDisplayName("WARRIOR"));
        assertEquals("Wizard", enumDef.getValueDisplayName("MAGE"));
    }

    @Test
    void registerSharedEnumFromStringValues() {
        wrapper.registerSharedEnum("Difficulty", "EASY", "NORMAL", "HARD");

        EnumDefinition enumDef = wrapper.getEnumRegistry().getEnum("Difficulty");
        assertEquals(List.of("EASY", "NORMAL", "HARD"), enumDef.getValues());
    }

    @Test
    void getEnumRegistryReturnsSameRegistryAfterRegistration() {
        EnumRegistry registry = wrapper.getEnumRegistry();
        wrapper.registerSharedEnum(ContextTestEnum.class);

        assertSame(registry, wrapper.getEnumRegistry());
        assertTrue(registry.hasEnum("ContextTestEnum"));
    }
}
