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
    public void wrappedReturnValuesCanBePassedBackToJavaMethods() {
        JavaContext context = new JavaContext();
        ValueHolder holder = new ValueHolder(7);
        context.register("holder", holder);

        LuaValue holderWrapper = context.toLuaTable().get("holder");
        LuaValue copied = holderWrapper.get("copy").call(holderWrapper);
        assertTrue(copied.istable());

        LuaValue accepted = holderWrapper.get("accept").call(holderWrapper, copied);
        assertTrue(accepted.toboolean());
    }
}
