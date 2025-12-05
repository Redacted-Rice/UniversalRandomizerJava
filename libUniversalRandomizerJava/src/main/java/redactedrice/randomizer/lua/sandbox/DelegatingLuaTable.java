package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

// Base class for lua table wrappers to control access/writing
// By default it just passes everything to the original
public abstract class DelegatingLuaTable extends LuaTable {
    protected final LuaTable original;

    protected DelegatingLuaTable(LuaTable original) {
        this.original = original;
    }

    @Override
    public LuaValue rawget(LuaValue key) {
        return original.rawget(key);
    }

    @Override
    public LuaValue get(LuaValue key) {
        return original.get(key);
    }

    @Override
    public LuaValue get(int key) {
        return original.get(key);
    }

    @Override
    public void set(LuaValue key, LuaValue value) {
        rawset(key, value);
    }

    @Override
    public Varargs next(LuaValue key) {
        return original.next(key);
    }

    @Override
    public int length() {
        return original.length();
    }
}
