package redactedrice.randomizer.lua.sandbox.security;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;

// Protects the global table from modification
public class GlobalsPolicy {

    public void applyToGlobals(Globals globals) {
        setupProtectedGlobals(globals);
    }

    private void setupProtectedGlobals(Globals globals) {
        // Create a metatable that prevents new global assignments
        LuaTable globalsMetatable = new LuaTable();

        globalsMetatable.set(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            // __newindex is only called for keys that don't exist in the table
            // so we can just block it to prevent new assignments
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                // Not sure its possible to pass nil here but guard for it just in case
                String keyStr = key.isnil() ? "nil" : key.tojstring();
                throw new SecurityException("Cannot create new global variable '" + keyStr
                        + "'. Global environment is protected. Use local variables instead.");
            }
        });
        // Don't modify __index so we can still access existing globals

        globals.setmetatable(globalsMetatable);
    }
}
