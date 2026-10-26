package redactedrice.randomizer.lua;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

// SecureLuaEnvironment and ScriptTestCase call this once per Globals before user scripts. LuaJ targets
// 5.2 but skips __pairs/__ipairs on pairs/ipairs.
public final class LuaBootstrap {
    private static final String BOOTSTRAP_RESOURCE = "luaj/fix_pairs_ipairs.lua";
    private static final String BOOTSTRAP_SOURCE = readBootstrapSource();

    private LuaBootstrap() {
    }

    public static void apply(Globals globals) {
        LuaValue chunk = globals.load(BOOTSTRAP_SOURCE, BOOTSTRAP_RESOURCE);
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
