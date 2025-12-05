package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

// Creates a read only table for lua so it can't be modified
public class ReadOnlyLuaTable extends DelegatingLuaTable {
    private final String tableName;

    public ReadOnlyLuaTable(LuaTable original, String tableName) {
        super(original);
        this.tableName = tableName;
    }

    @Override
    public void rawset(LuaValue key, LuaValue value) {
        throw new SecurityException("Cannot modify package: " + tableName);
    }
}
