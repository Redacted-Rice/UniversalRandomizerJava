package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.utils.LuaJavaConverter;

import java.util.Map;

// Converts validated Java arguments to a Lua table for module execution. All argument types
// (including list and table) are passed through as plain Lua tables with no URC wrapper objects.
public class ModuleArgumentConverter {

    public LuaTable toLuaTable(Map<String, Object> arguments) {
        LuaTable table = new LuaTable();

        if (arguments != null) {
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                LuaValue luaValue = LuaJavaConverter.javaToLua(entry.getValue());
                table.set(entry.getKey(), luaValue);
            }
        }

        return table;
    }
}
