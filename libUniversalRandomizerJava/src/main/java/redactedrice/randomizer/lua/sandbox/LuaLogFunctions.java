package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import redactedrice.randomizer.utils.Logger;

import java.util.HashSet;
import java.util.Set;

// lua function wrappers for logging that modules can call via the logger table
public class LuaLogFunctions {
    private static String concatenateArgs(Varargs args) {
        // if no args return empty string
        if (args.narg() == 0) {
            return "";
        }

        // concatenate all arguments with tab separators like lua does
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= args.narg(); i++) {
            if (i > 1) {
                sb.append("\t");
            }
            sb.append(args.arg(i).tojstring());
        }
        return sb.toString();
    }

    public static LuaValue createDebugFunction() {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Logger.debug(concatenateArgs(args));
                return LuaValue.NIL;
            }
        };
    }

    public static LuaValue createInfoFunction() {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Logger.info(concatenateArgs(args));
                return LuaValue.NIL;
            }
        };
    }

    public static LuaValue createWarnFunction() {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Logger.warn(concatenateArgs(args));
                return LuaValue.NIL;
            }
        };
    }

    public static LuaValue createErrorFunction() {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Logger.error(concatenateArgs(args));
                return LuaValue.NIL;
            }
        };
    }

    public static LuaValue createTableToStringFunction() {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() == 0) {
                    return LuaValue.valueOf("nil");
                }

                LuaValue value = args.arg(1);
                int maxDepth = 1; // Default depth

                if (args.narg() >= 2 && args.arg(2).isnumber()) {
                    maxDepth = args.arg(2).toint();
                }

                String result = luaValueToString(value, maxDepth, 0, new HashSet<>());
                return LuaValue.valueOf(result);
            }
        };
    }

    private static String luaValueToString(LuaValue value, int maxDepth, int currentDepth,
            Set<LuaValue> visited) {
        // handle primitive types
        if (value.isnil()) {
            return "nil";
        }

        if (value.isboolean()) {
            return value.toboolean() ? "true" : "false";
        }

        if (value.isnumber()) {
            return value.tojstring();
        }

        if (value.isstring()) {
            return "\"" + value.tojstring() + "\"";
        }

        // handle tables recursively
        if (value.istable()) {
            // check if we've exceeded max depth unless maxDepth is negative for unlimited
            if (maxDepth >= 0 && currentDepth >= maxDepth) {
                return "{...}";
            }

            LuaTable table = value.checktable();

            // check for cycles to avoid infinite recursion
            if (visited.contains(value)) {
                return "{circular}";
            }

            // Add to visited set
            Set<LuaValue> newVisited = new HashSet<>(visited);
            newVisited.add(value);

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            boolean first = true;

            // Iterate over table entries
            LuaValue key = LuaValue.NIL;
            while (true) {
                Varargs entry = table.next(key);
                key = entry.arg1();
                if (key.isnil()) {
                    break;
                }

                LuaValue val = entry.arg(2);

                if (!first) {
                    sb.append(", ");
                }
                first = false;

                // Format key
                if (key.isnumber()) {
                    int index = key.toint();
                    // Check if it's an array-like index
                    if (index > 0 && key.todouble() == index) {
                        // Array-style: just show the value
                        sb.append(luaValueToString(val, maxDepth, currentDepth + 1, newVisited));
                    } else {
                        // Non-sequential number key
                        sb.append("[").append(key.tojstring()).append("] = ");
                        sb.append(luaValueToString(val, maxDepth, currentDepth + 1, newVisited));
                    }
                } else if (key.isstring()) {
                    String keyStr = key.tojstring();
                    // Check if key is a valid identifier
                    if (keyStr.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                        sb.append(keyStr).append(" = ");
                    } else {
                        sb.append("[\"").append(keyStr).append("\"] = ");
                    }
                    sb.append(luaValueToString(val, maxDepth, currentDepth + 1, newVisited));
                } else {
                    // Other key types
                    sb.append("[").append(key.tojstring()).append("] = ");
                    sb.append(luaValueToString(val, maxDepth, currentDepth + 1, newVisited));
                }
            }

            sb.append("}");
            return sb.toString();
        }

        if (value.isfunction()) {
            return "<function>";
        }

        if (value.isuserdata()) {
            return "<userdata: " + value.tojstring() + ">";
        }

        if (value.isthread()) {
            return "<thread>";
        }

        return value.tojstring();
    }
}
