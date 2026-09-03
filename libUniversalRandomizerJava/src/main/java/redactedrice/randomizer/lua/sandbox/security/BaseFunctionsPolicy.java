package redactedrice.randomizer.lua.sandbox.security;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

// Removes dangerous base functions and restricts the debug library
public class BaseFunctionsPolicy {

    public void applyToGlobals(Globals globals) {
        removeBlockedModules(globals);
        restrictDebugLibrary(globals);
        removeDangerousBaseFunctions(globals);
    }

    private void removeBlockedModules(Globals globals) {
        // Remove blocked modules completely
        // Note this does NOT include debug intentionally
        for (String module : SandboxModulePolicy.REMOVED_MODULES) {
            globals.set(module, LuaValue.NIL);
        }
    }

    private void restrictDebugLibrary(Globals globals) {
        // restrict debug table to only have traceback
        LuaValue debugLib = globals.get("debug");
        if (!debugLib.isnil() && debugLib.istable()) {
            LuaTable restrictedDebug = new LuaTable();
            LuaValue traceback = debugLib.get("traceback");
            if (!traceback.isnil()) {
                // keep traceback so we get good error messages
                restrictedDebug.set("traceback", traceback);
            }
            globals.set("debug", restrictedDebug);
        } else {
            globals.set("debug", LuaValue.NIL);
        }
    }

    private void removeDangerousBaseFunctions(Globals globals) {
        // remove dangerous base functions
        globals.set("dofile", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);
        globals.set("loadstring", LuaValue.NIL);

        // remove lua 5.1 environment manipulation
        globals.set("getfenv", LuaValue.NIL);
        globals.set("setfenv", LuaValue.NIL);

        // Remove collectgarbage. No reason to have it and it can be abused. Probably a bit overkill
        // but it was simple to add so I decided just to do it.
        globals.set("collectgarbage", LuaValue.NIL);

        // Block rawset and rawget to prevent bypassing our protections
        // I am not sure if rawget is really needed but there also isn't really
        // any reason why they should be using it so I remove it to be safe
        globals.set("rawset", LuaValue.NIL);
        globals.set("rawget", LuaValue.NIL);
    }
}
