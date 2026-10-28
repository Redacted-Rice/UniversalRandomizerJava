package redactedrice.randomizer.utils;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import redactedrice.randomizer.context.EnumRegistry;
import redactedrice.randomizer.context.JavaObjectWrapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LuaJavaConverterTest {

    public static class Payload {
        public String id = "one";
    }

    private enum SampleEnum {
        A, B
    }

    @Test
    public void coerceJavaLuaValueWrapsNestedJavaObjects() {
        JavaObjectWrapper wrapper = new JavaObjectWrapper(new EnumRegistry());
        Payload payload = new Payload();
        LuaValue raw = CoerceJavaToLua.coerce(payload);

        LuaValue coerced = LuaJavaConverter.coerceJavaLuaValue(raw, wrapper);

        assertTrue(coerced.istable(), "nested objects should become extensible wrappers");
        assertTrue(coerced.get("id").isstring());
    }

    @Test
    public void coerceJavaLuaValueLeavesEnumsAsUserdata() {
        JavaObjectWrapper wrapper = new JavaObjectWrapper(new EnumRegistry());
        LuaValue raw = CoerceJavaToLua.coerce(SampleEnum.A);

        LuaValue coerced = LuaJavaConverter.coerceJavaLuaValue(raw, wrapper);

        assertTrue(coerced.isuserdata());
        assertSame(SampleEnum.A, coerced.touserdata());
    }

    @Test
    public void luaToJavaUnwrapsWrapperTableToOriginalInstance() {
        JavaObjectWrapper objectWrapper = new JavaObjectWrapper(new EnumRegistry());
        Payload payload = new Payload();
        LuaValue wrapped = objectWrapper.wrap(payload);

        Object back = LuaJavaConverter.luaToJava(wrapped);

        assertSame(payload, back);
    }

    @Test
    public void luaToJavaSkipTablesDoesNotUnwrapWrapper() {
        JavaObjectWrapper objectWrapper = new JavaObjectWrapper(new EnumRegistry());
        Payload payload = new Payload();
        LuaTable wrapped = (LuaTable) objectWrapper.wrap(payload);

        Object back = LuaJavaConverter.luaToJava(wrapped, true);

        assertTrue(back instanceof LuaTable);
        assertSame(wrapped, back);
    }
}
