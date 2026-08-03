package redactedrice.randomizer.lua.arguments;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.utils.LuaJavaConverter;
import redactedrice.randomizer.utils.IssueTracker;

import java.util.ArrayList;
import java.util.List;

public class ArgumentParser {

    public static List<ArgumentDefinition> parseArgumentsFromTable(LuaTable moduleTable,
            String context) {
        LuaValue argsValue = moduleTable.get("arguments");
        if (argsValue.isnil()) {
            // If its not present, return empty list
            return new ArrayList<>();
        }
        if (!argsValue.istable()) {
            IssueTracker.addError(context + " 'arguments' field must be a table");
            return null;
        }
        return parseArguments(argsValue.checktable(), context);
    }

    private static List<ArgumentDefinition> parseArguments(LuaTable argsTable, String context) {
        List<ArgumentDefinition> arguments = new ArrayList<>();

        // walk through the array part of the lua table
        LuaValue key = LuaValue.NIL;
        while (true) {
            key = argsTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }

            LuaValue argValue = argsTable.get(key);
            if (!argValue.istable()) {
                IssueTracker.addError(context + " argument entry must be a table");
                return null;
            }

            // parse each argument definition
            try {
                ArgumentDefinition argDef = parseArgumentDefinition(argValue.checktable(), context);
                if (argDef != null) {
                    arguments.add(argDef);
                }
            } catch (Exception e) {
                IssueTracker.addError(context + " error parsing argument: " + e.getMessage());
                return null;
            }
        }

        return arguments;
    }

    private static ArgumentDefinition parseArgumentDefinition(LuaTable argTable, String context) {
        String name = LuaJavaConverter.tryGetStringFromTable(argTable, "name", null, context);
        if (name == null || name.trim().isEmpty()) {
            IssueTracker.addError(context + " argument missing 'name' field");
            return null;
        }

        // get the type definition which can be string or table
        LuaValue definitionValue = argTable.get("definition");
        if (definitionValue.isnil()) {
            IssueTracker.addError(context + " argument '" + name + "' missing 'definition' field");
            return null;
        }

        TypeDefinition typeDef;
        try {
            if (definitionValue.isstring()) {
                // simple type like "number" or "string"
                typeDef = TypeDefinition.parse(definitionValue.tojstring());
            } else if (definitionValue.istable()) {
                // complex type with constraints embedded
                typeDef = TypeDefinition.parse(LuaJavaConverter.luaToJava(definitionValue));
            } else {
                IssueTracker.addError(
                        context + " argument '" + name + "' has invalid definition field");
                return null;
            }
        } catch (IllegalArgumentException e) {
            IssueTracker.addError(context + " invalid argument definition: " + e.getMessage());
            return null;
        }

        // get default value if present
        LuaValue defaultValue = argTable.get("default");
        Object javaDefaultValue = null;
        if (!defaultValue.isnil()) {
            javaDefaultValue = LuaJavaConverter.luaToJava(defaultValue);
        }

        return new ArgumentDefinition(name, typeDef, javaDefaultValue);
    }
}
