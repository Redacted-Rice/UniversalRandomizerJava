package redactedrice.randomizer.context;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaObjectWrapperTest {

    public static class Card {
        public String name;
        public int hp;

        public Card(String name, int hp) {
            this.name = name;
            this.hp = hp;
        }

        public List<Card> getCards() {
            List<Card> cards = new ArrayList<>();
            cards.add(new Card("a", 10));
            cards.add(new Card("b", 20));
            return cards;
        }
    }

    public static class TypeA {
        public String label = "A";
    }

    public static class TypeB {
        public String label = "B";
    }

    public static class DualTypeProcessor {
        public String lastType;

        public void process(TypeA value) {
            lastType = "A";
        }

        public void process(TypeB value) {
            lastType = "B";
        }
    }

    public static class OverloadProbe {
        public String lastCall;

        public void process(int value) {
            lastCall = "int";
        }

        public void process(String value) {
            lastCall = "string";
        }
    }

    public static class ValueHolder {
        private int value;

        public ValueHolder(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public ValueHolder copy() {
            return new ValueHolder(value);
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean accept(ValueHolder other) {
            return other != null && other.value == value;
        }

        public boolean accept(ValueHolder other, boolean force) {
            return other != null && (force || other.value == value);
        }
    }

    @Test
    public void methodReturnedListItemsAreExtensibleWrappers() {
        JavaContext context = new JavaContext();
        Card root = new Card("root", 1);
        context.register("root", root);

        LuaTable luaContext = context.toLuaTable();
        LuaValue rootWrapper = luaContext.get("root");
        assertTrue(rootWrapper.istable());

        LuaValue cards = rootWrapper.get("getCards").call(rootWrapper);
        assertTrue(cards.istable());

        LuaValue first = cards.get(1);
        assertTrue(first.istable(), "list items should be wrapper tables, not userdata");

        first.set("tier", LuaValue.valueOf(2));
        assertEquals(2, first.get("tier").toint());
        assertEquals("a", first.get("name").tojstring());
    }

    @Test
    public void clearWrapperCacheDropsDynamicFields() {
        JavaContext context = new JavaContext();
        Card root = new Card("root", 1);
        context.register("root", root);

        LuaValue rootWrapper = context.toLuaTable().get("root");
        rootWrapper.set("scratch", LuaValue.valueOf("keep_me"));
        assertEquals("keep_me", rootWrapper.get("scratch").tojstring());

        context.clearWrapperCache();

        LuaValue freshWrapper = context.toLuaTable().get("root");
        assertTrue(freshWrapper.get("scratch").isnil());
        assertEquals("root", freshWrapper.get("name").tojstring());
    }

    @Test
    public void wrappedReturnValuesWorkWithJavaMethodOverloads() {
        JavaContext context = new JavaContext();
        ValueHolder holder = new ValueHolder(7);
        ValueHolder different = new ValueHolder(99);
        context.register("holder", holder);
        context.register("different", different);

        LuaValue holderWrapper = context.toLuaTable().get("holder");
        LuaValue differentWrapper = context.toLuaTable().get("different");
        LuaValue copied = holderWrapper.get("copy").call(holderWrapper);
        assertTrue(copied.istable());

        LuaValue accepted = holderWrapper.get("accept").call(holderWrapper, copied);
        assertTrue(accepted.toboolean());

        LuaValue rejected = holderWrapper.get("accept").call(holderWrapper, differentWrapper);
        assertFalse(rejected.toboolean());

        LuaValue forced = holderWrapper.get("accept").call(holderWrapper, differentWrapper,
                LuaValue.TRUE);
        assertTrue(forced.toboolean());
    }

    @Test
    public void overloadResolutionSelectsPrimitiveVsString() {
        JavaContext context = new JavaContext();
        OverloadProbe probe = new OverloadProbe();
        context.register("probe", probe);

        LuaValue wrapper = context.toLuaTable().get("probe");
        wrapper.get("process").call(wrapper, LuaValue.valueOf(42));
        assertEquals("int", probe.lastCall);

        wrapper.get("process").call(wrapper, LuaValue.valueOf("hello"));
        assertEquals("string", probe.lastCall);
    }

    @Test
    public void overloadResolutionDistinguishesWrappedJavaTypes() {
        JavaContext context = new JavaContext();
        DualTypeProcessor processor = new DualTypeProcessor();
        TypeA typeA = new TypeA();
        TypeB typeB = new TypeB();
        context.register("processor", processor);
        context.register("typeA", typeA);
        context.register("typeB", typeB);

        LuaTable luaContext = context.toLuaTable();
        LuaValue processorWrapper = luaContext.get("processor");
        LuaValue typeAWrapper = luaContext.get("typeA");
        LuaValue typeBWrapper = luaContext.get("typeB");

        processorWrapper.get("process").call(processorWrapper, typeBWrapper);
        assertEquals("B", processor.lastType);

        processorWrapper.get("process").call(processorWrapper, typeAWrapper);
        assertEquals("A", processor.lastType);

        processorWrapper.get("process").call(processorWrapper, typeBWrapper);
        assertEquals("B", processor.lastType);
    }

    @Test
    public void tablesWithNonUserdataFieldAreNotUnwrapped() {
        JavaContext context = new JavaContext();
        ValueHolder holder = new ValueHolder(7);
        context.register("holder", holder);

        LuaValue holderWrapper = context.toLuaTable().get("holder");
        LuaTable fakeWrapper = new LuaTable();
        fakeWrapper.set("__userdata", LuaValue.valueOf("not-java"));

        assertThrows(Exception.class,
                () -> holderWrapper.get("accept").call(holderWrapper, fakeWrapper));
    }
}
