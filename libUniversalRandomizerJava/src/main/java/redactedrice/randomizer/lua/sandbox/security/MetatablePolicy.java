package redactedrice.randomizer.lua.sandbox.security;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

// Restricts setmetatable to prevent modification of protected tables
public class MetatablePolicy {

    public void applyToGlobals(Globals globals) {
        setupRestrictedSetMetatable(globals);
    }

    private void setupRestrictedSetMetatable(Globals globals) {
        final LuaValue originalSetmetatable = globals.get("setmetatable");
        final Globals globalsFinal = globals;
        globals.set("setmetatable", createSetmetatableWrapper(originalSetmetatable, globalsFinal));
    }

    private TwoArgFunction createSetmetatableWrapper(LuaValue originalSetmetatable,
            Globals globals) {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue metatable) {
                validateSetmetatableArguments(table, metatable);
                validateSetmetatableTarget(table, globals);
                // Allow setmetatable for all other tables
                return originalSetmetatable.call(table, metatable);
            }
        };
    }

    private void validateSetmetatableArguments(LuaValue table, LuaValue metatable) {
        // Handle bad arguments gracefully
        if (table.isnil()) {
            throw new IllegalArgumentException("setmetatable: table cannot be nil");
        }
        if (!table.istable()) {
            throw new IllegalArgumentException(
                    "setmetatable: first argument must be a table, got " + table.typename());
        }
        if (!metatable.isnil() && !metatable.istable()) {
            throw new IllegalArgumentException(
                    "setmetatable: metatable must be nil or a table, got " + metatable.typename());
        }
    }

    private void validateSetmetatableTarget(LuaValue table, Globals globals) {
        // Prevent modification of globals table metatable
        if (table == globals || (table.istable() && table.touserdata() == globals)) {
            throw new SecurityException("Cannot modify metatable of global environment");
        }

        // Prevent modification of package system tables metatables
        validatePackageTableMetatable(table, globals);
    }

    private void validatePackageTableMetatable(LuaValue table, Globals globals) {
        LuaValue packageLib = globals.get("package");
        if (packageLib.isnil() || !packageLib.istable()) {
            return;
        }

        LuaTable packageTable = (LuaTable) packageLib;
        String[] protectedPackageTables = {"loaded", "loaders", "searchers", "preload"};

        for (String tableName : protectedPackageTables) {
            LuaValue protectedTable = packageTable.get(tableName);
            if (!protectedTable.isnil()
                    && (table == protectedTable || table.equals(protectedTable))) {
                throw new SecurityException(
                        "Cannot modify metatable of protected " + tableName + " table");
            }
        }
    }
}
