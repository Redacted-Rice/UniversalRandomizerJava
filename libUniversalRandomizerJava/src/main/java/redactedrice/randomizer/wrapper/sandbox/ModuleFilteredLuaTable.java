package redactedrice.randomizer.wrapper.sandbox;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Set;

// Creates a table that blocks certain modules from being accessed or modified
public class ModuleFilteredLuaTable extends DelegatingLuaTable {
    private final Set<String> blockedModules;

    public ModuleFilteredLuaTable(LuaTable original, Set<String> blockedModules) {
        super(original);
        this.blockedModules = blockedModules;
    }

    @Override
    public void rawset(LuaValue key, LuaValue value) {
        // Only allow setting if module name is not blocked
        if (key.isstring()) {
            String moduleName = key.tojstring();
            if (isBlockedModuleName(moduleName)) {
                throw new SecurityException("Cannot load blocked module:" + moduleName);
            }
        }
        original.rawset(key, value);
    }

    @Override
    public LuaValue rawget(LuaValue key) {
        // Check if trying to access a blocked module
        if (key.isstring()) {
            String moduleName = key.tojstring();
            if (isBlockedModuleName(moduleName)) {
                // Return nil instead of allowing access to potentially injected modules
                return LuaValue.NIL;
            }
        }
        return original.rawget(key);
    }

    @Override
    public LuaValue get(LuaValue key) {
        // Use rawget to apply filtering
        return rawget(key);
    }

    private boolean isBlockedModuleName(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return false;
        }

        String normalized = moduleName.trim();
        return blockedModules.contains(normalized) || normalized.equals("debug");
    }
}
