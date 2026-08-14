package redactedrice.randomizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.context.testsupport.TestEnergyType;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.support.test.EnumFieldTestCard;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomizerEnumSelectTest {

    private LuaSandbox sandbox;
    private JavaContext context;

    @BeforeEach
    public void setUp() {
        String randomizerPath =
                new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        sandbox = new LuaSandbox(List.of(randomizerPath));

        context = new JavaContext();
        context.registerEnum(TestEnergyType.class);
        context.register("cards", Arrays.asList(new EnumFieldTestCard("ember", TestEnergyType.FIRE),
                new EnumFieldTestCard("splash", TestEnergyType.WATER),
                new EnumFieldTestCard("flame", TestEnergyType.FIRE)));
        context.register("target", new EnumFieldTestCard("target", TestEnergyType.FIRE));

        sandbox.set("context", context.toLuaTable());
    }

    @Test
    public void selectAndGroupingPreserveEnumUserdataWhileKeysStringify() {
        LuaValue result = sandbox.execute(buildEnumRoundTripScript());
        LuaTable checks = result.checktable();

        assertTrue(checks.get("getValueIsUserdata").toboolean(),
                "getValue should return enum userdata");
        assertTrue(checks.get("getValueNotString").toboolean(),
                "getValue should not stringify enums");
        assertTrue(checks.get("selectIsUserdata").toboolean(),
                "select should preserve enum userdata");
        assertTrue(checks.get("asTableKeyIsString").toboolean(),
                "asTableKey should stringify enum userdata for keys");
        assertEquals("FIRE", checks.get("asTableKeyName").tojstring());
        assertEquals(2, checks.get("fireGroupSize").toint());
        assertTrue(checks.get("userdataLookupMisses").toboolean(),
                "Group:get should use string keys after enum groupBy");

        EnumFieldTestCard target = (EnumFieldTestCard) context.get("target");
        assertNotNull(target.getType());
        String assignedName = checks.get("targetTypeName").tojstring();
        assertTrue("FIRE".equals(assignedName) || "WATER".equals(assignedName),
                "useToRandomize should assign enum userdata, got " + assignedName);
    }

    @Test
    public void groupedUseToRandomizeMatchesEnumSelectorKeys() {
        context.register("targets", Arrays.asList(new EnumFieldTestCard("a", TestEnergyType.FIRE),
                new EnumFieldTestCard("b", TestEnergyType.WATER)));
        sandbox.set("context", context.toLuaTable());

        String script = """
                local randomizer = require("randomizer")
                local utils = require("randomizer.utils")
                local cards = context.cards
                local targets = context.targets

                local pools = randomizer.groupFromField(cards, "getType", "getType")
                randomizer.setSeed(99)
                pools:useToRandomize(targets, "getType", "setType")

                return {
                    fireIsFire = tostring(utils.getValue(targets[1], "getType")) == "FIRE",
                    waterIsWater = tostring(utils.getValue(targets[2], "getType")) == "WATER",
                }
                """;

        LuaTable checks = sandbox.execute(script).checktable();

        assertTrue(checks.get("fireIsFire").toboolean());
        assertTrue(checks.get("waterIsWater").toboolean());

        @SuppressWarnings("unchecked")
        List<EnumFieldTestCard> targets = (List<EnumFieldTestCard>) context.get("targets");
        assertEquals(TestEnergyType.FIRE, targets.get(0).getType());
        assertEquals(TestEnergyType.WATER, targets.get(1).getType());
    }

    private static String buildEnumRoundTripScript() {
        return """
                local randomizer = require("randomizer")
                local utils = require("randomizer.utils")
                local cards = context.cards
                local target = context.target

                local raw = utils.getValue(cards[1], "getType")
                local results = {}
                results.getValueIsUserdata = (type(raw) == "userdata")
                results.getValueNotString = (type(raw) ~= "string")

                local selected = randomizer.list(cards):select("getType"):get(1)
                results.selectIsUserdata = (type(selected) == "userdata")

                local key = utils.asTableKey(raw)
                results.asTableKeyIsString = (type(key) == "string")
                results.asTableKeyName = key

                local grouped = randomizer.groupBy(cards, "getType")
                results.fireGroupSize = grouped:get("FIRE"):size()
                results.userdataLookupMisses = (grouped:get(raw) == nil)

                randomizer.setSeed(42)
                local pool = randomizer.list(cards):select("getType"):removeDuplicates()
                pool:useToRandomize({ target }, "setType")
                results.targetTypeName = tostring(utils.getValue(target, "getType"))

                return results
                """;
    }
}
