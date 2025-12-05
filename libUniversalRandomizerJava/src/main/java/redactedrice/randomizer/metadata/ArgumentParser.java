package redactedrice.randomizer.metadata;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.logger.ErrorTracker;
import redactedrice.randomizer.wrapper.LuaParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArgumentParser {

    public static List<ArgumentDefinition> parseArgumentsFromTable(LuaTable moduleTable,
            String context) {
        LuaValue argsValue = moduleTable.get("arguments");
        if (argsValue.isnil()) {
            // If its not present, return empty list
            return new ArrayList<>();
        }
        if (!argsValue.istable()) {
            ErrorTracker.addError(context + " 'arguments' field must be a table");
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
                ErrorTracker.addError(context + " argument entry must be a table");
                return null;
            }

            // parse each argument definition
            try {
                ArgumentDefinition argDef = parseArgumentDefinition(argValue.checktable(), context);
                if (argDef != null) {
                    arguments.add(argDef);
                }
            } catch (Exception e) {
                ErrorTracker.addError(context + " error parsing argument: " + e.getMessage());
                return null;
            }
        }

        return arguments;
    }

    private static ArgumentDefinition parseArgumentDefinition(LuaTable argTable, String context) {
        String name = LuaParser.parseString(argTable, "name", null, context);
        if (name == null || name.trim().isEmpty()) {
            ErrorTracker.addError(context + " argument missing 'name' field");
            return null;
        }

        // get the type definition which can be string or table
        LuaValue definitionValue = argTable.get("definition");
        if (definitionValue.isnil()) {
            ErrorTracker.addError(context + " argument '" + name + "' missing 'definition' field");
            return null;
        }

        TypeDefinition typeDef;
        try {
            if (definitionValue.isstring()) {
                // simple type like "number" or "string"
                typeDef = TypeDefinition.parse(definitionValue.tojstring());
            } else if (definitionValue.istable()) {
                // complex type with constraints embedded
                Map<String, Object> defMap =
                        (Map<String, Object>) LuaParser.luaValueToJava(definitionValue);
                typeDef = TypeDefinition.parse(defMap);
            } else {
                ErrorTracker.addError(
                        context + " argument '" + name + "' has invalid definition field");
                return null;
            }
        } catch (IllegalArgumentException e) {
            ErrorTracker.addError(context + " invalid argument definition: " + e.getMessage());
            return null;
        }

        // get default value if present
        LuaValue defaultValue = argTable.get("default");
        Object javaDefaultValue = null;
        if (!defaultValue.isnil()) {
            javaDefaultValue = LuaParser.luaValueToJava(defaultValue);
        }

        return new ArgumentDefinition(name, typeDef, javaDefaultValue);
    }
}
