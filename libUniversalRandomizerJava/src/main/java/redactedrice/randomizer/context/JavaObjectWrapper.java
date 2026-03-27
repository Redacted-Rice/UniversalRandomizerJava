package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// Wraps Java objects in Lua tables with metatable-based method access
// Provides extensible wrapper that allows both Java method calls and dynamic Lua fields
public class JavaObjectWrapper {
    private final EnumRegistry enumRegistry;

    public JavaObjectWrapper(EnumRegistry enumRegistry) {
        this.enumRegistry = enumRegistry;
    }

    // Wrap a Java object in a Lua table with method interception
    public LuaValue wrap(Object javaObject) {
        if (javaObject == null) {
            return LuaValue.NIL;
        }

        LuaValue userdata = CoerceJavaToLua.coerce(javaObject);

        // Create an extensible wrapper table
        LuaTable wrapper = new LuaTable();

        Map<String, Method> methodCache = new HashMap<>();

        // Create metatable for forwarding to Java object
        LuaTable metatable = new LuaTable();

        // __index: Try wrapper first, then userdata
        metatable.set(LuaValue.INDEX, new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                // First check the wrapper table itself
                LuaValue wrapperValue = wrapper.rawget(key);
                if (!wrapperValue.isnil()) {
                    return wrapperValue;
                }

                // Then try the userdata (Java object)
                try {
                    LuaValue userdataValue = userdata.get(key);

                    // If it's a function, wrap it to convert string enum parameters
                    if (userdataValue.isfunction()) {
                        return new EnumMethodInterceptor(javaObject, key.toString(), userdataValue,
                                userdata, methodCache, enumRegistry);
                    }

                    return userdataValue;
                } catch (Exception e) {
                    return LuaValue.NIL;
                }
            }
        });

        // __newindex: Always store in wrapper table for extensibility
        metatable.set(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                // Try to set on userdata first (for actual Java fields)
                try {
                    userdata.set(key, value);
                } catch (Throwable e) {
                    // If that fails, store in wrapper (for dynamic Lua fields)
                    // Must catch Throwable because LuaJ throws LuaError
                    wrapper.rawset(key, value);
                }
                return LuaValue.NIL;
            }
        });

        // Store reference to underlying userdata for debugging
        wrapper.rawset("__userdata", userdata);

        // Apply metatable
        wrapper.setmetatable(metatable);

        return wrapper;
    }
}
