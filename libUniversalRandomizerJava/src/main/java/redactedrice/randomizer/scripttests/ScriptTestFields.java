package redactedrice.randomizer.scripttests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.LuaJavaConverter;

// Apply and assert Lua spec tables onto wrapped Java objects.
// Assignment goes through the same wrapper/setters live Lua uses.
public final class ScriptTestFields {
    private ScriptTestFields() {}

    public static void apply(JavaContext context, Object javaObject, Map<String, Object> spec) {
        applyTarget(context, wrap(context, javaObject), spec);
    }

    public static void collectMismatches(JavaContext context, Object javaObject,
            Map<String, Object> spec, List<String> mismatches, String path) {
        collectFromTarget(context, wrap(context, javaObject), spec, mismatches, path);
    }

    public static void failIfMismatches(String label, List<String> mismatches) {
        if (mismatches == null || mismatches.isEmpty()) {
            return;
        }
        throw new IllegalStateException(label + " " + String.join(". ", mismatches));
    }

    private static LuaValue wrap(JavaContext context, Object javaObject) {
        if (javaObject instanceof LuaValue lua) {
            return lua;
        }
        return context.wrap(javaObject);
    }

    private static void applyTarget(JavaContext context, LuaValue target, Map<String, Object> spec) {
        if (spec == null || spec.isEmpty()) {
            return;
        }
        List<String> scalars = new ArrayList<>();
        List<String> maps = new ArrayList<>();
        List<String> lists = new ArrayList<>();
        for (String key : spec.keySet()) {
            Object value = spec.get(key);
            if (isListOfMaps(value)) {
                lists.add(key);
            } else if (value instanceof Map<?, ?>) {
                maps.add(key);
            } else {
                scalars.add(key);
            }
        }
        for (String key : scalars) {
            applyScalar(context, target, key, spec.get(key));
        }
        for (String key : maps) {
            applyMap(context, target, key, ScriptTestValues.optionalMap(spec.get(key)));
        }
        for (String key : lists) {
            applyList(context, target, key,
                    ScriptTestValues.optionalListOfMaps(spec.get(key), key));
        }
    }

    private static void collectFromTarget(JavaContext context, LuaValue target,
            Map<String, Object> spec, List<String> mismatches, String path) {
        if (spec == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : spec.entrySet()) {
            collectMismatch(context, target, entry.getKey(), entry.getValue(), mismatches, path);
        }
    }

    private static void applyScalar(JavaContext context, LuaValue target, String key,
            Object value) {
        LuaValue luaValue = toLua(value);
        LuaValue current = asTarget(context, target.get(key));
        // Lua strings throw if you index them. Only wrapped text objects have setText.
        if (!isNil(current) && isString(value) && !current.isstring()
                && isFunction(current.get("setText"))) {
            invoke(current.get("setText"), current, LuaValue.valueOf(String.valueOf(value)));
            return;
        }
        LuaValue setter = target.get(setterName(key));
        if (isFunction(setter)) {
            invoke(setter, target, luaValue);
            return;
        }
        target.set(key, luaValue);
    }

    private static void applyMap(JavaContext context, LuaValue target, String key,
            Map<String, Object> values) {
        LuaValue clearer = target.get("clear" + cap(key));
        LuaValue setter = target.get(setterName(singular(key)));
        if (isFunction(setter) && isScalarMap(values)) {
            if (isFunction(clearer)) {
                invoke(clearer, target);
            }
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                invoke(setter, target, LuaValue.valueOf(entry.getKey()), toLua(entry.getValue()));
            }
            return;
        }

        LuaValue nested = asTarget(context, target.get(key));
        if (!isNil(nested) && (nested.istable() || nested.isuserdata())) {
            applyTarget(context, nested, values);
            return;
        }
        target.set(key, LuaJavaConverter.mapToLuaTable(values));
    }

    private static void applyList(JavaContext context, LuaValue target, String key,
            List<Map<String, Object>> entries) {
        LuaValue setCount = target.get("setNum" + cap(key));
        if (isFunction(setCount)) {
            invoke(setCount, target, LuaValue.valueOf(entries.size()));
        }

        String itemName = singular(key);
        LuaValue getter = target.get("get" + cap(itemName));
        LuaValue setter = target.get("set" + cap(itemName));
        if (!isFunction(getter)) {
            throw new IllegalArgumentException(
                    "Cannot apply list '" + key + "'. No get" + cap(itemName) + " method");
        }

        for (int i = 0; i < entries.size(); i++) {
            LuaValue item = asTarget(context, invoke(getter, target, LuaValue.valueOf(i)).arg1());
            if (isNil(item)) {
                throw new IllegalArgumentException(
                        "get" + cap(itemName) + "(" + i + ") returned nil");
            }
            applyTarget(context, item, entries.get(i));
            if (isFunction(setter)) {
                // true = force set even for assignments
                invoke(setter, target, item, LuaValue.valueOf(i), LuaValue.TRUE);
            }
        }
    }

    private static void collectMismatch(JavaContext context, LuaValue target, String key,
            Object expected, List<String> mismatches, String path) {
        String fieldPath = path + " " + key;
        if (isListOfMaps(expected)) {
            List<Map<String, Object>> wanted =
                    ScriptTestValues.optionalListOfMaps(expected, key);
            LuaValue getCount = target.get("getNum" + cap(key));
            if (isFunction(getCount)) {
                int actualCount = invoke(getCount, target).arg1().toint();
                if (actualCount != wanted.size()) {
                    mismatches.add(fieldPath + " count expected " + wanted.size() + " but was "
                            + actualCount);
                    return;
                }
            }
            LuaValue getter = target.get("get" + cap(singular(key)));
            if (!isFunction(getter)) {
                mismatches.add(fieldPath + " has no get" + cap(singular(key)) + " method");
                return;
            }
            for (int i = 0; i < wanted.size(); i++) {
                LuaValue item =
                        asTarget(context, invoke(getter, target, LuaValue.valueOf(i)).arg1());
                collectFromTarget(context, item, wanted.get(i), mismatches,
                        fieldPath + "[" + (i + 1) + "]");
            }
            return;
        }

        if (expected instanceof Map<?, ?>) {
            Map<String, Object> wanted = ScriptTestValues.optionalMap(expected);
            String getterName = "get" + cap(singular(key));
            LuaValue getter = target.get(getterName);
            if (isFunction(getter) && isScalarMap(wanted)) {
                collectEnumKeyedMismatches(target, getterName, getter, wanted, mismatches,
                        fieldPath);
                return;
            }
            LuaValue nested = asTarget(context, read(target, key));
            if (isNil(nested)) {
                mismatches.add(fieldPath + " expected a nested object but was missing");
                return;
            }
            collectFromTarget(context, nested, wanted, mismatches, fieldPath);
            return;
        }

        LuaValue actual = read(target, key);
        if (!valuesMatch(expected, actual)) {
            mismatches.add(fieldPath + " expected " + expected + " but was " + describe(actual));
        }
    }

    private static void collectEnumKeyedMismatches(LuaValue target, String getterName,
            LuaValue getter, Map<String, Object> wanted, List<String> mismatches, String path) {
        Class<?> enumClass = enumParamType(javaObject(target), getterName);
        if (enumClass != null && enumClass.isEnum()) {
            for (Object constant : enumClass.getEnumConstants()) {
                String name = ((Enum<?>) constant).name();
                Object expected = wanted.containsKey(name) ? wanted.get(name) : 0;
                LuaValue actual = invoke(getter, target, LuaValue.valueOf(name)).arg1();
                if (!valuesMatch(expected, actual)) {
                    mismatches.add(path + " " + name + " expected " + expected + " but was "
                            + describe(actual));
                }
            }
            return;
        }
        for (Map.Entry<String, Object> entry : wanted.entrySet()) {
            LuaValue actual = invoke(getter, target, LuaValue.valueOf(entry.getKey())).arg1();
            if (!valuesMatch(entry.getValue(), actual)) {
                mismatches.add(path + " " + entry.getKey() + " expected " + entry.getValue()
                        + " but was " + describe(actual));
            }
        }
    }

    private static LuaValue read(LuaValue target, String key) {
        LuaValue getter = target.get("get" + cap(key));
        if (isFunction(getter)) {
            return invoke(getter, target).arg1();
        }
        return target.get(key);
    }

    private static Class<?> enumParamType(Object java, String methodName) {
        if (java == null) {
            return null;
        }
        for (Method method : java.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param.isEnum()) {
                return param;
            }
        }
        return null;
    }

    private static Object javaObject(LuaValue target) {
        if (isNil(target)) {
            return null;
        }
        LuaValue userdata = target.get("__userdata");
        if (userdata.isuserdata()) {
            return userdata.touserdata();
        }
        if (target.isuserdata()) {
            return target.touserdata();
        }
        return null;
    }

    private static LuaValue asTarget(JavaContext context, LuaValue value) {
        if (isNil(value)) {
            return value;
        }
        if (value.isuserdata()) {
            Object java = value.touserdata();
            if (java != null && !(java instanceof String) && !isPrimitive(java)
                    && !(java instanceof Enum)) {
                return context.wrap(java);
            }
        }
        return value;
    }

    private static Varargs invoke(LuaValue function, LuaValue... args) {
        return function.invoke(LuaValue.varargsOf(args));
    }

    private static LuaValue toLua(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        return LuaJavaConverter.javaToLua(value);
    }

    private static boolean valuesMatch(Object expected, LuaValue actualLua) {
        if (isNil(actualLua)) {
            return expected == null;
        }
        Object actual = LuaJavaConverter.luaToJava(actualLua);
        if (expected instanceof Number expectedNumber && actual instanceof Number actualNumber) {
            if (expectedNumber instanceof Double || expectedNumber instanceof Float
                    || actualNumber instanceof Double || actualNumber instanceof Float) {
                return Double.compare(expectedNumber.doubleValue(),
                        actualNumber.doubleValue()) == 0;
            }
            return expectedNumber.longValue() == actualNumber.longValue();
        }
        return String.valueOf(expected).equals(String.valueOf(actual));
    }

    private static String describe(LuaValue value) {
        if (isNil(value)) {
            return "nil";
        }
        return String.valueOf(LuaJavaConverter.luaToJava(value));
    }

    private static boolean isListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return false;
        }
        return list.isEmpty() || list.get(0) instanceof Map<?, ?>;
    }

    private static boolean isScalarMap(Map<String, Object> values) {
        if (values.isEmpty()) {
            return false;
        }
        for (Object value : values.values()) {
            if (value instanceof Map<?, ?> || value instanceof List<?>) {
                return false;
            }
        }
        return true;
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }

    private static boolean isFunction(LuaValue value) {
        return value != null && value.isfunction();
    }

    private static boolean isNil(LuaValue value) {
        return value == null || value.isnil();
    }

    private static boolean isPrimitive(Object value) {
        return value instanceof Boolean || value instanceof Number || value instanceof Character;
    }

    private static String setterName(String key) {
        return "set" + cap(key);
    }

    private static String cap(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String singular(String name) {
        if (name != null && name.length() > 1 && name.endsWith("s") && !name.endsWith("ss")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }
}
