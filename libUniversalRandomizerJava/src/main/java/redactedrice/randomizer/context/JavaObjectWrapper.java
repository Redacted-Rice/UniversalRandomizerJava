package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Wraps Java objects in Lua tables with metatable-based method access
// Provides extensible wrapper that allows both Java method calls and dynamic Lua fields
public class JavaObjectWrapper {
    private final EnumRegistry enumRegistry;
    // We need to cache objects so we can store and keep lua assigned values to them
    private final Map<Object, LuaTable> wrapperCache = new IdentityHashMap<>();
    // Name to type from module provides. Used when Lua assigns a string to a dynamic field.
    private final Map<String, String> dynamicFieldTypes = new LinkedHashMap<>();

    public JavaObjectWrapper(EnumRegistry enumRegistry) {
        this.enumRegistry = enumRegistry;
    }

    public void mergeDynamicFieldTypes(Map<String, String> types) {
        if (types == null || types.isEmpty()) {
            return;
        }
        dynamicFieldTypes.putAll(types);
    }

    /** Clears cached wrappers so a new randomization does not reuse stale dynamic Lua fields */
    public void clearCache() {
        wrapperCache.clear();
    }

    // Wrap a Java object in a Lua table with method interception
    public LuaValue wrap(Object javaObject) {
        if (javaObject == null) {
            return LuaValue.NIL;
        }

        LuaTable cached = wrapperCache.get(javaObject);
        if (cached != null) {
            return cached;
        }

        LuaValue userdata = CoerceJavaToLua.coerce(javaObject);
        LuaTable wrapper = new LuaTable();
        Map<String, Method> methodCache = new HashMap<>();

        LuaTable metatable = new LuaTable();
        metatable.set(LuaValue.INDEX, new WrapperIndex(javaObject, userdata, wrapper, methodCache,
                enumRegistry, this));
        metatable.set(LuaValue.NEWINDEX, new WrapperNewIndex(userdata, wrapper, this));

        wrapper.rawset("__userdata", userdata);
        wrapper.setmetatable(metatable);

        wrapperCache.put(javaObject, wrapper);
        return wrapper;
    }

    /** __index: wrapper fields first, then Java userdata / intercepted methods */
    private static final class WrapperIndex extends TwoArgFunction {
        private final Object javaObject;
        private final LuaValue userdata;
        private final LuaTable wrapper;
        private final Map<String, Method> methodCache;
        private final EnumRegistry enumRegistry;
        private final JavaObjectWrapper objectWrapper;

        WrapperIndex(Object javaObject, LuaValue userdata, LuaTable wrapper,
                Map<String, Method> methodCache, EnumRegistry enumRegistry,
                JavaObjectWrapper objectWrapper) {
            this.javaObject = javaObject;
            this.userdata = userdata;
            this.wrapper = wrapper;
            this.methodCache = methodCache;
            this.enumRegistry = enumRegistry;
            this.objectWrapper = objectWrapper;
        }

        @Override
        public LuaValue call(LuaValue table, LuaValue key) {
            LuaValue wrapperValue = wrapper.rawget(key);
            if (!wrapperValue.isnil()) {
                return wrapperValue;
            }

            try {
                LuaValue userdataValue = userdata.get(key);
                if (userdataValue.isfunction()) {
                    return new EnumMethodInterceptor(javaObject, key.toString(), userdataValue,
                            userdata, methodCache, enumRegistry, objectWrapper);
                }
                return userdataValue;
            } catch (Exception e) {
                return LuaValue.NIL;
            }
        }
    }

    /** __newindex: Java fields when possible, otherwise dynamic Lua fields on the wrapper */
    private static final class WrapperNewIndex extends ThreeArgFunction {
        private final LuaValue userdata;
        private final LuaTable wrapper;
        private final JavaObjectWrapper objectWrapper;

        WrapperNewIndex(LuaValue userdata, LuaTable wrapper, JavaObjectWrapper objectWrapper) {
            this.userdata = userdata;
            this.wrapper = wrapper;
            this.objectWrapper = objectWrapper;
        }

        @Override
        public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
            LuaValue toSet = coerceAssignedValue(key, value);
            try {
                userdata.set(key, toSet);
            } catch (Throwable e) {
                // Must catch Throwable because LuaJ throws LuaError
                wrapper.rawset(key, toSet);
            }
            return LuaValue.NIL;
        }

        private LuaValue coerceAssignedValue(LuaValue key, LuaValue value) {
            if (!value.isstring() || !key.isstring()) {
                return value;
            }
            String keyName = key.tojstring();
            String valueName = value.tojstring();
            Object enumValue = coerceJavaEnumField(keyName, valueName);
            if (enumValue == null) {
                enumValue = objectWrapper.coerceProvidedEnum(keyName, valueName);
            }
            if (enumValue == null) {
                return value;
            }
            return CoerceJavaToLua.coerce(enumValue);
        }

        private Object coerceJavaEnumField(String keyName, String valueName) {
            if (!userdata.isuserdata()) {
                return null;
            }
            Object java = userdata.touserdata();
            if (java == null) {
                return null;
            }
            Field field = findPublicField(java.getClass(), keyName);
            if (field == null || !field.getType().isEnum()) {
                return null;
            }
            return objectWrapper.enumRegistry.stringToEnum(field.getType(), valueName);
        }
    }

    private Object coerceProvidedEnum(String fieldName, String valueName) {
        String typeName = dynamicFieldTypes.get(fieldName);
        if (typeName == null) {
            return null;
        }
        return enumRegistry.stringToEnum(typeName, valueName);
    }

    private static Field findPublicField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
