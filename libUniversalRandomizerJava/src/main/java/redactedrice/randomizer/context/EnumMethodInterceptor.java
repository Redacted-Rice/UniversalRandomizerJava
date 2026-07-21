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
    private final JavaObjectWrapper objectWrapper;

    public EnumMethodInterceptor(Object javaObject, String methodName, LuaValue originalMethod,
            LuaValue userdata, Map<String, Method> methodCache, EnumRegistry enumRegistry,
            JavaObjectWrapper objectWrapper) {
        this.javaObject = javaObject;
        this.methodName = methodName;
        this.originalMethod = originalMethod;
        this.userdata = userdata;
        this.methodCache = methodCache;
        this.enumRegistry = enumRegistry;
        this.objectWrapper = objectWrapper;
    }

    @Override
    public Varargs invoke(Varargs args) {
        // Try to find the Java method using reflection
        Method javaMethod = findJavaMethod(javaObject.getClass(), methodName, args.narg() - 1, args);

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
            LuaValue arg = unwrapJavaWrapper(args.arg(i + 1));
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
            newArgs[i] = unwrapJavaWrapper(args.arg(i + 1));
        }
        return newArgs;
    }

    private static LuaValue unwrapJavaWrapper(LuaValue arg) {
        if (arg.istable()) {
            LuaValue inner = arg.get("__userdata");
            if (!inner.isnil()) {
                return inner;
            }
        }
        return arg;
    }

    private Varargs convertReturnValue(Varargs result) {
        // Convert return value if it's a Java collection or other complex object so nested
        // objects stay as extensible wrappers (dynamic Lua fields on cards, etc.)
        LuaValue firstValue = result.narg() > 0 ? result.arg1() : LuaValue.NIL;
        if (firstValue.isuserdata()) {
            Object returned = firstValue.touserdata();
            if (returned instanceof java.util.List || returned instanceof java.util.Map) {
                return LuaJavaConverter.javaToLua(returned, objectWrapper);
            }
            if (objectWrapper != null && returned != null && !(returned instanceof String)
                    && !isPrimitiveOrWrapper(returned) && !(returned instanceof Enum)) {
                return objectWrapper.wrap(returned);
            }
        }
        return result;
    }

    private static boolean isPrimitiveOrWrapper(Object value) {
        return value instanceof Boolean || value instanceof Byte || value instanceof Character
                || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double;
    }

    private Method findJavaMethod(Class<?> clazz, String methodName, int paramCount, Varargs args) {
        String cacheKey = clazz.getName() + "#" + methodName + "#" + paramCount + "#"
                + argumentSignature(args);
        Method cached = methodCache.get(cacheKey);
        if (cached != null || methodCache.containsKey(cacheKey)) {
            return cached;
        }

        Method bestMatch = null;
        int bestScore = Integer.MIN_VALUE;
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != paramCount) {
                continue;
            }
            int score = scoreMethodMatch(method, args);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = method;
            }
        }

        methodCache.put(cacheKey, bestMatch);
        return bestMatch;
    }

    private static String argumentSignature(Varargs args) {
        StringBuilder signature = new StringBuilder();
        for (int i = 2; i <= args.narg(); i++) {
            if (signature.length() > 0) {
                signature.append(',');
            }
            signature.append(args.arg(i).typename());
        }
        return signature.toString();
    }

    private int scoreMethodMatch(Method method, Varargs args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            LuaValue arg = unwrapJavaWrapper(args.arg(i + 2));
            score += scoreArgumentMatch(arg, paramTypes[i]);
        }
        return score;
    }

    private int scoreArgumentMatch(LuaValue arg, Class<?> paramType) {
        if (paramType.isPrimitive()) {
            if (paramType == boolean.class) {
                return arg.isboolean() ? 10 : Integer.MIN_VALUE / 2;
            }
            if (paramType == int.class || paramType == long.class || paramType == double.class
                    || paramType == float.class || paramType == short.class
                    || paramType == byte.class || paramType == char.class) {
                return arg.isnumber() ? 10 : Integer.MIN_VALUE / 2;
            }
        }
        if (paramType == Boolean.class) {
            return arg.isboolean() ? 10 : 0;
        }
        if (Number.class.isAssignableFrom(paramType)) {
            return arg.isnumber() ? 10 : 0;
        }
        if (paramType.isEnum()) {
            if (arg.isstring()) {
                return 10;
            }
            if (arg.isuserdata(paramType)) {
                return 10;
            }
            return 0;
        }
        if (arg.isnil()) {
            return !paramType.isPrimitive() ? 5 : Integer.MIN_VALUE / 2;
        }
        if (arg.isuserdata()) {
            Object userdata = arg.touserdata();
            if (userdata != null && paramType.isAssignableFrom(userdata.getClass())) {
                return 10;
            }
            return 0;
        }
        if (arg.isstring() && paramType == String.class) {
            return 10;
        }
        if (arg.isboolean() && paramType == Boolean.class) {
            return 10;
        }
        return 1;
    }
}
