package redactedrice.randomizer.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import redactedrice.randomizer.lua.sandbox.LuaSandbox;

public class LuaBootstrapTest {

    @Test
    void pairsHonorsPairsMetamethod() {
        Globals globals = JsePlatform.standardGlobals();
        LuaBootstrap.apply(globals);

        LuaValue result = globals.load("""
                local t = setmetatable({}, {
                    __pairs = function()
                        local done = false
                        return function()
                            if not done then
                                done = true
                                return "key", 42
                            end
                        end
                    end,
                })
                local value = nil
                for _, v in pairs(t) do
                    value = v
                end
                return value
                """).call();

        assertEquals(42, result.toint());
    }

    @Test
    void pairsHonorsGroupMetatableInSandbox() {
        String randomizerPath =
                new File("../UniversalRandomizerCore/randomizer").getAbsolutePath();
        LuaSandbox sandbox = new LuaSandbox(java.util.List.of(randomizerPath));

        LuaValue result = sandbox.execute("""
                local randomizer = require("randomizer")
                local grouped = randomizer.group({ a = { 1, 2 }, b = { 3 } })
                local count = 0
                for key, list in pairs(grouped) do
                    count = count + list:size()
                end
                return count
                """);

        assertEquals(3, result.toint());
    }
}
