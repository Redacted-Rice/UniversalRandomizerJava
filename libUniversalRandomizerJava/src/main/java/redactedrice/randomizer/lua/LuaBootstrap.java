package redactedrice.randomizer.lua;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

// Runs once per Globals before user scripts. Installs Lua 5.2 style pairs/ipairs for LuaJ for more consistent
// and expected behavior
public final class LuaBootstrap {
    private static final String BOOTSTRAP_RESOURCE = "luaj/fix_pairs_ipairs.lua";

    private LuaBootstrap() {
    }

    public static void apply(Globals globals) {
        String source = readBootstrapSource();
        LuaValue chunk = globals.load(source, BOOTSTRAP_RESOURCE);
        chunk.call();
    }

    private static String readBootstrapSource() {
        ClassLoader loader = LuaBootstrap.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(BOOTSTRAP_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing classpath resource " + BOOTSTRAP_RESOURCE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + BOOTSTRAP_RESOURCE, e);
        }
    }
}
