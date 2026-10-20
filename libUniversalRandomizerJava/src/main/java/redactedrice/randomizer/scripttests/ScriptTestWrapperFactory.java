package redactedrice.randomizer.scripttests;

import redactedrice.randomizer.LuaRandomizerWrapper;

// Called once per case in ScriptTestBatchRunner. Return a fresh wrapper unless you know
// reuse is safe. Lua state and module registry do not reset between cases on the same instance.
@FunctionalInterface
public interface ScriptTestWrapperFactory {
    LuaRandomizerWrapper create();
}
