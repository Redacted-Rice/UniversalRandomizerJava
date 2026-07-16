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
}
