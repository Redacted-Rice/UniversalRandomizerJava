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
            if (ScriptTestValues.isListFieldSpec(value)) {
                lists.add(key);
            } else if (value instanceof Map<?, ?> map && ScriptTestValues.isKeyedMapSpec(map)) {
                maps.add(key);
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
                    ScriptTestValues.parseListFieldSpec(spec.get(key), key));
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
        if (ScriptTestValues.isKeyedMapSpec(values)) {
            applyKeyedMap(target, ScriptTestValues.parseKeyedMapSpec(values, key));
            return;
        }

        LuaValue nested = asTarget(context, target.get(key));
        if (!isNil(nested) && (nested.istable() || nested.isuserdata())) {
            applyTarget(context, nested, values);
            return;
        }
        target.set(key, LuaJavaConverter.mapToLuaTable(values));
    }

    private static void applyKeyedMap(LuaValue target, ScriptTestValues.KeyedMapSpec spec) {
        invokeHooks(target, spec.pre());
        LuaValue setter = target.get(spec.setterMethod());
        if (!isFunction(setter)) {
            throw new IllegalArgumentException(
                    "No " + spec.setterMethod() + " method for keyed map");
        }
        if (spec.accessType() == ScriptTestValues.AccessType.WHOLE) {
            invoke(setter, target, LuaJavaConverter.mapToLuaTable(spec.entries()));
        } else {
            for (Map.Entry<String, Object> entry : spec.entries().entrySet()) {
                invoke(setter, target, LuaValue.valueOf(entry.getKey()), toLua(entry.getValue()));
            }
        }
        invokeHooks(target, spec.post());
    }

    private static void applyList(JavaContext context, LuaValue target, String key,
            ScriptTestValues.ListFieldSpec list) {
        invokeHooks(target, list.pre());

        List<Map<String, Object>> entries = list.values();
        if (list.accessType() == ScriptTestValues.AccessType.ITEM) {
            applyItemList(context, target, key, list, entries);
        } else {
            applyWholeList(context, target, key, list, entries);
        }

        invokeHooks(target, list.post());
    }

    private static void applyItemList(JavaContext context, LuaValue target, String key,
            ScriptTestValues.ListFieldSpec list, List<Map<String, Object>> entries) {
        if (list.countSetterMethod() != null) {
            LuaValue setCount = target.get(list.countSetterMethod());
            if (isFunction(setCount)) {
                invoke(setCount, target, LuaValue.valueOf(entries.size()));
            }
        }

        LuaValue getter = target.get(list.getterMethod());
        LuaValue setter = target.get(list.setterMethod());
        if (!isFunction(getter)) {
            throw new IllegalArgumentException(
                    "Cannot apply list '" + key + "'. No " + list.getterMethod() + " method");
        }

        for (int i = 0; i < entries.size(); i++) {
            LuaValue item = asTarget(context, invoke(getter, target, LuaValue.valueOf(i)).arg1());
            if (isNil(item)) {
                throw new IllegalArgumentException(
                        list.getterMethod() + "(" + i + ") returned nil");
            }
            applyTarget(context, item, entries.get(i));
            if (isFunction(setter)) {
                // true = force set even for assignments
                invoke(setter, target, item, LuaValue.valueOf(i), LuaValue.TRUE);
            }
        }
    }

    private static void applyWholeList(JavaContext context, LuaValue target, String key,
            ScriptTestValues.ListFieldSpec list, List<Map<String, Object>> entries) {
        String itemGetterName = list.itemGetterMethod();
        if (itemGetterName == null) {
            throw new IllegalArgumentException(
                    "Cannot apply whole list '" + key + "'. Needs item getter from field name");
        }
        LuaValue itemGetter = target.get(itemGetterName);
        if (!isFunction(itemGetter)) {
            throw new IllegalArgumentException(
                    "Cannot apply whole list '" + key + "'. No " + itemGetterName + " method");
        }

        List<LuaValue> built = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LuaValue item = asTarget(context, invoke(itemGetter, target, LuaValue.valueOf(i)).arg1());
            if (isNil(item)) {
                throw new IllegalArgumentException(itemGetterName + "(" + i + ") returned nil");
            }
            applyTarget(context, item, entries.get(i));
            built.add(item);
        }

        LuaValue setter = target.get(list.setterMethod());
        if (!isFunction(setter)) {
            throw new IllegalArgumentException(
                    "Cannot apply whole list '" + key + "'. No " + list.setterMethod() + " method");
        }
        invoke(setter, target, toLuaList(built));
    }

    private static void collectMismatch(JavaContext context, LuaValue target, String key,
            Object expected, List<String> mismatches, String path) {
        String fieldPath = path + " " + key;
        if (ScriptTestValues.isListFieldSpec(expected)) {
            ScriptTestValues.ListFieldSpec list =
                    ScriptTestValues.parseListFieldSpec(expected, key);
            List<Map<String, Object>> wanted = list.values();
            if (list.accessType() == ScriptTestValues.AccessType.ITEM) {
                collectItemListMismatches(context, target, list, wanted, mismatches, fieldPath);
            } else {
                collectWholeListMismatches(context, target, list, wanted, mismatches, fieldPath);
            }
            return;
        }

        if (expected instanceof Map<?, ?>) {
            Map<String, Object> wanted = ScriptTestValues.optionalMap(expected);
            if (ScriptTestValues.isKeyedMapSpec(wanted)) {
                collectKeyedMapMismatches(target,
                        ScriptTestValues.parseKeyedMapSpec(wanted, key), mismatches, fieldPath);
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

    private static void collectItemListMismatches(JavaContext context, LuaValue target,
            ScriptTestValues.ListFieldSpec list, List<Map<String, Object>> wanted,
            List<String> mismatches, String fieldPath) {
        if (list.countGetterMethod() != null) {
            LuaValue getCount = target.get(list.countGetterMethod());
            if (isFunction(getCount)) {
                int actualCount = invoke(getCount, target).arg1().toint();
                if (actualCount != wanted.size()) {
                    mismatches.add(fieldPath + " count expected " + wanted.size() + " but was "
                            + actualCount);
                    return;
                }
            }
        }
        LuaValue getter = target.get(list.getterMethod());
        if (!isFunction(getter)) {
            mismatches.add(fieldPath + " has no " + list.getterMethod() + " method");
            return;
        }
        for (int i = 0; i < wanted.size(); i++) {
            LuaValue item = asTarget(context, invoke(getter, target, LuaValue.valueOf(i)).arg1());
            collectFromTarget(context, item, wanted.get(i), mismatches,
                    fieldPath + "[" + (i + 1) + "]");
        }
    }

    private static void collectWholeListMismatches(JavaContext context, LuaValue target,
            ScriptTestValues.ListFieldSpec list, List<Map<String, Object>> wanted,
            List<String> mismatches, String fieldPath) {
        String itemGetterName = list.itemGetterMethod();
        if (itemGetterName == null) {
            mismatches.add(fieldPath + " whole list has no item getter from field name");
            return;
        }
        LuaValue itemGetter = target.get(itemGetterName);
        if (!isFunction(itemGetter)) {
            mismatches.add(fieldPath + " has no " + itemGetterName + " method");
            return;
        }
        for (int i = 0; i < wanted.size(); i++) {
            LuaValue item = asTarget(context,
                    invoke(itemGetter, target, LuaValue.valueOf(i)).arg1());
            if (isNil(item)) {
                mismatches.add(fieldPath + " count expected " + wanted.size() + " but was " + i);
                return;
            }
            collectFromTarget(context, item, wanted.get(i), mismatches,
                    fieldPath + "[" + (i + 1) + "]");
        }
        if (hasListItemAt(itemGetter, target, wanted.size())) {
            mismatches.add(fieldPath + " count expected " + wanted.size() + " but was at least "
                    + (wanted.size() + 1));
        }
    }

    private static boolean hasListItemAt(LuaValue itemGetter, LuaValue target, int index) {
        try {
            return !isNil(invoke(itemGetter, target, LuaValue.valueOf(index)).arg1());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void collectKeyedMapMismatches(LuaValue target,
            ScriptTestValues.KeyedMapSpec spec, List<String> mismatches, String path) {
        String getterName = spec.getterMethod();
        LuaValue getter = target.get(getterName);
        if (!isFunction(getter)) {
            mismatches.add(path + " has no " + getterName + " method");
            return;
        }
        if (spec.accessType() == ScriptTestValues.AccessType.WHOLE) {
            collectWholeMapMismatches(target, getter, spec.entries(), mismatches, path);
            return;
        }
        collectEnumKeyedMismatches(target, getterName, getter, spec.entries(), mismatches, path);
    }

    private static void collectWholeMapMismatches(LuaValue target, LuaValue getter,
            Map<String, Object> wanted, List<String> mismatches, String path) {
        LuaValue actualMap = invoke(getter, target).arg1();
        if (!actualMap.istable()) {
            mismatches.add(path + " expected a map but was " + describe(actualMap));
            return;
        }
        for (Map.Entry<String, Object> entry : wanted.entrySet()) {
            LuaValue actual = actualMap.get(entry.getKey());
            if (!valuesMatch(entry.getValue(), actual)) {
                mismatches.add(path + " " + entry.getKey() + " expected " + entry.getValue()
                        + " but was " + describe(actual));
            }
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

    private static void invokeHooks(LuaValue target, List<String> hookNames) {
        for (String hookName : hookNames) {
            LuaValue hook = target.get(hookName);
            if (!isFunction(hook)) {
                throw new IllegalArgumentException("No hook method '" + hookName + "'");
            }
            invoke(hook, target);
        }
    }

    private static LuaValue read(LuaValue target, String key) {
        LuaValue direct = target.get(key);
        if (isFunction(direct)) {
            return invoke(direct, target).arg1();
        }
        LuaValue getter = target.get("get" + cap(key));
        if (isFunction(getter)) {
            return invoke(getter, target).arg1();
        }
        getter = target.get("is" + cap(key));
        if (isFunction(getter)) {
            return invoke(getter, target).arg1();
        }
        return direct;
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

    private static LuaValue toLuaList(List<LuaValue> items) {
        LuaValue table = LuaValue.tableOf();
        for (int i = 0; i < items.size(); i++) {
            table.set(i + 1, items.get(i));
        }
        return table;
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
        if (expected == null) {
            return false;
        }
        if (expected instanceof Number expectedNumber && actual instanceof Number actualNumber) {
            if (expectedNumber instanceof Double || expectedNumber instanceof Float
                    || actualNumber instanceof Double || actualNumber instanceof Float) {
                return Double.compare(expectedNumber.doubleValue(),
                        actualNumber.doubleValue()) == 0;
            }
            return expectedNumber.longValue() == actualNumber.longValue();
        }
        if (expected instanceof Boolean expectedBool) {
            return actual instanceof Boolean actualBool && expectedBool.equals(actualBool);
        }
        if (actual instanceof Boolean) {
            return false;
        }
        if (expected instanceof String expectedString && actual instanceof Enum<?> actualEnum) {
            return expectedString.equals(actualEnum.name());
        }
        if (expected instanceof Enum<?> expectedEnum) {
            if (actual instanceof Enum<?> actualEnum) {
                return expectedEnum == actualEnum;
            }
            return expectedEnum.name().equals(String.valueOf(actual));
        }
        return String.valueOf(expected).equals(String.valueOf(actual));
    }

    private static String describe(LuaValue value) {
        if (isNil(value)) {
            return "nil";
        }
        return String.valueOf(LuaJavaConverter.luaToJava(value));
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
}
