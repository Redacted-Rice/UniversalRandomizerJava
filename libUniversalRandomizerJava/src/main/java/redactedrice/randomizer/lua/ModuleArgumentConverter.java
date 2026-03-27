package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;
import redactedrice.randomizer.lua.arguments.ArgumentType;
import redactedrice.randomizer.lua.arguments.TypeDefinition;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.utils.LuaJavaConverter;

import java.util.HashMap;
import java.util.Map;

// Converts validated Java arguments to Lua table format
// Handles special cases like GROUP types that need wrapping
public class ModuleArgumentConverter {
    private final LuaSandbox sandbox;

    public ModuleArgumentConverter(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
    }

    // Convert arguments to Lua table format
    public LuaTable toLuaTable(Module module, Map<String, Object> arguments) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }

        LuaTable table = new LuaTable();

        if (arguments != null) {
            // build a map of argument name to type definition for quick lookup
            // this is needed to handle GROUP types specially
            Map<String, TypeDefinition> argTypes = buildTypeMap(module);

            // convert each argument to Lua format
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String argName = entry.getKey();
                Object value = entry.getValue();

                // check if this is a GROUP type argument
                // GROUP types need special handling because they need to be wrapped
                TypeDefinition argType = argTypes.get(argName);
                if (argType != null && argType.getBaseType() == ArgumentType.GROUP) {
                    convertGroupArgument(table, argName, value);
                } else {
                    // regular conversion for non group types
                    LuaValue luaValue = LuaJavaConverter.javaToLua(value);
                    table.set(argName, luaValue);
                }
            }
        }

        return table;
    }

    private Map<String, TypeDefinition> buildTypeMap(Module module) {
        Map<String, TypeDefinition> argTypes = new HashMap<>();
        for (ArgumentDefinition argDef : module.getArguments()) {
            argTypes.put(argDef.getName(), argDef.getTypeDefinition());
        }
        return argTypes;
    }

    private void convertGroupArgument(LuaTable table, String argName, Object value) {
        // for group types convert the map to a lua table then wrap it with randomizer
        // group
        try {
            LuaValue mapTable = LuaJavaConverter.javaToLua(value);

            // get the randomizer module and group function
            LuaValue randomizerModule =
                    sandbox.getGlobals().get("require").call(LuaValue.valueOf("randomizer"));
            LuaValue groupFunction = randomizerModule.get("group");

            if (groupFunction.isnil()) {
                throw new IllegalStateException(
                        "randomizer.group function not found. Make sure randomizer module is properly loaded.");
            }

            // call randomizer group on the table and set the result
            LuaValue groupObject = groupFunction.call(mapTable);
            table.set(argName, groupObject);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to convert argument '" + argName + "' to Group: " + e.getMessage(), e);
        }
    }
}
