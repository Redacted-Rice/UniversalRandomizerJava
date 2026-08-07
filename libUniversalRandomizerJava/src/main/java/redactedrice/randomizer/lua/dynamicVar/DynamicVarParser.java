package redactedrice.randomizer.lua.dynamicVar;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.randomizer.utils.LuaJavaConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Parses provides and needs metadata arrays from module tables. values are not read here. */
public final class DynamicVarParser {
    private DynamicVarParser() {}

    public static List<DynamicVar> parseFromTable(LuaTable moduleTable, String fieldName,
            String context) {
        LuaValue values = moduleTable.get(fieldName);
        if (values.isnil()) {
            return List.of();
        }
        if (!values.istable()) {
            IssueTracker.addError(context + " field '" + fieldName + "' must be a table");
            return List.of();
        }
        return parseEntries(values.checktable(), fieldName, context);
    }

    private static List<DynamicVar> parseEntries(LuaTable entriesTable, String fieldName,
            String context) {
        List<DynamicVar> entries = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        LuaValue key = LuaValue.NIL;
        while (true) {
            key = entriesTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }

            LuaValue entryValue = entriesTable.get(key);
            if (!entryValue.istable()) {
                IssueTracker.addError(context + " " + fieldName + " entry must be a table");
                return List.of();
            }

            DynamicVar entry = parseEntry(entryValue.checktable(), fieldName, context, seenNames);
            if (entry == null) {
                return List.of();
            }
            entries.add(entry);
        }

        return List.copyOf(entries);
    }

    private static DynamicVar parseEntry(LuaTable entryTable, String fieldName, String context,
            Set<String> seenNames) {
        String name = LuaJavaConverter.tryGetStringFromTable(entryTable, "name", null, context);
        if (name == null || name.trim().isEmpty()) {
            IssueTracker.addError(context + " " + fieldName + " entry missing 'name' field");
            return null;
        }

        if (!seenNames.add(name.trim())) {
            IssueTracker.addError(
                    context + " " + fieldName + " declares duplicate name '" + name + "'");
            return null;
        }

        String type = LuaJavaConverter.tryGetStringFromTable(entryTable, "type", null, context);
        if (type == null || type.trim().isEmpty()) {
            IssueTracker.addError(
                    context + " " + fieldName + " entry '" + name + "' missing 'type' field");
            return null;
        }

        return new DynamicVar(name, type);
    }
}
