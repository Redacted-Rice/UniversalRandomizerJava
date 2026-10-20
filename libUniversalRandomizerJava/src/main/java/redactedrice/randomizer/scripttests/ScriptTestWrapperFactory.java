package redactedrice.randomizer.scripttests;

import redactedrice.randomizer.LuaRandomizerWrapper;

@FunctionalInterface
public interface ScriptTestWrapperFactory {
    LuaRandomizerWrapper create();
}
