package redactedrice.randomizer.context;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import redactedrice.randomizer.utils.LuaJavaConverter;

import java.lang.reflect.Method;
import java.util.Map;

// Intercepts method calls to convert string parameters to enum values
// Uses reflection to determine parameter types and converts accordingly
public class EnumMethodInterceptor extends VarArgFunction {
    private final Object javaObject;
    private final String methodName;
    private final LuaValue originalMethod;
    private final LuaValue userdata;
    private final Map<String, Method> methodCache;
    private final EnumRegistry enumRegistry;

    public EnumMethodInterceptor(Object javaObject, String methodName, LuaValue originalMethod,
            LuaValue userdata, Map<String, Method> methodCache, EnumRegistry enumRegistry) {
        this.javaObject = javaObject;
        this.methodName = methodName;
        this.originalMethod = originalMethod;
        this.userdata = userdata;
        this.methodCache = methodCache;
        this.enumRegistry = enumRegistry;
    }

    @Override
    public Varargs invoke(Varargs args) {
        // Try to find the Java method using reflection
        Method javaMethod = findJavaMethod(javaObject.getClass(), methodName, args.narg() - 1);

        // Always use userdata as 'self' (first argument)
        LuaValue self = determineSelf(args);

        if (javaMethod != null) {
            // Convert arguments, converting strings to enums when appropriate
            LuaValue[] newArgs = convertArguments(args, javaMethod.getParameterTypes(), self);
            Varargs result = originalMethod.invoke(LuaValue.varargsOf(newArgs));
            return convertReturnValue(result);
        } else {
            // Method not found via reflection, call original method as-is
            LuaValue[] newArgs = buildArgsArray(args, self);
            Varargs result = originalMethod.invoke(LuaValue.varargsOf(newArgs));
            return convertReturnValue(result);
        }
    }

    private LuaValue determineSelf(Varargs args) {
        if (args.narg() > 0 && args.arg(1).istable()) {
            // Check if it's our wrapper by looking for __userdata field
            LuaValue wrapperUserdata = args.arg(1).get("__userdata");
            if (!wrapperUserdata.isnil() && wrapperUserdata == userdata) {
                return userdata;
            } else {
                return args.arg(1);
            }
        }
        return userdata;
    }

    private LuaValue[] convertArguments(Varargs args, Class<?>[] paramTypes, LuaValue self) {
        LuaValue[] newArgs = new LuaValue[args.narg()];
        newArgs[0] = self;

        // Convert remaining args
        for (int i = 1; i < args.narg(); i++) {
            LuaValue arg = args.arg(i + 1);
            int paramIndex = i - 1; // Parameter index (0-based, excluding 'self')

            if (paramIndex < paramTypes.length && paramTypes[paramIndex].isEnum()) {
                newArgs[i] = convertEnumArgument(arg, paramTypes[paramIndex]);
            } else {
                newArgs[i] = arg;
            }
        }

        return newArgs;
    }

    private LuaValue convertEnumArgument(LuaValue arg, Class<?> enumClass) {
        // This parameter is an enum - try to convert string to enum
        if (arg.isstring()) {
            String stringValue = arg.tojstring();
            Object enumValue = enumRegistry.stringToEnum(enumClass.getSimpleName(), stringValue);
            if (enumValue == null) {
                // Try with custom enum names registered in EnumRegistry
                for (String enumName : enumRegistry.getEnumNames()) {
                    enumValue = enumRegistry.stringToEnum(enumName, stringValue);
                    if (enumValue != null && enumValue.getClass() == enumClass) {
                        break;
                    }
                }
            }
            if (enumValue != null) {
                return CoerceJavaToLua.coerce(enumValue);
            }
        }
        return arg; // Keep original if conversion fails or not a string
    }

    private LuaValue[] buildArgsArray(Varargs args, LuaValue self) {
        LuaValue[] newArgs = new LuaValue[args.narg()];
        newArgs[0] = self;
        for (int i = 1; i < args.narg(); i++) {
            newArgs[i] = args.arg(i + 1);
        }
        return newArgs;
    }

    private Varargs convertReturnValue(Varargs result) {
        // Convert return value if it's a Java collection
        LuaValue firstValue = result.narg() > 0 ? result.arg1() : LuaValue.NIL;
        if (firstValue.isuserdata()) {
            Object javaObject = firstValue.touserdata();
            if (javaObject instanceof java.util.List || javaObject instanceof java.util.Map) {
                return LuaJavaConverter.javaToLua(javaObject);
            }
        }
        return result;
    }

    private Method findJavaMethod(Class<?> clazz, String methodName, int paramCount) {
        String cacheKey = clazz.getName() + "#" + methodName + "#" + paramCount;
        Method cached = methodCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Search for method
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                methodCache.put(cacheKey, method);
                return method;
            }
        }

        // Cache null result to avoid repeated searches
        methodCache.put(cacheKey, null);
        return null;
    }
}
