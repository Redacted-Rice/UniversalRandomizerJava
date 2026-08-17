package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import redactedrice.randomizer.utils.LuaJavaConverter;

// One Lua case file. Standard keys are module, args, and seed. The rest is host data.
public final class ScriptTestCase {
    private final Path source;
    private final Map<String, Object> data;

    private ScriptTestCase(Path source, Map<String, Object> data) {
        this.source = source;
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public static ScriptTestCase load(Path caseFile) throws IOException {
        String lua = Files.readString(caseFile, StandardCharsets.UTF_8);
        Globals globals = JsePlatform.standardGlobals();
        LuaValue chunk = globals.load(lua, caseFile.getFileName().toString());
        Object loaded = LuaJavaConverter.luaToJava(chunk.call());
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    caseFile + " must return a table with a module field");
        }
        return new ScriptTestCase(caseFile, (Map<String, Object>) map);
    }

    public Path source() {
        return source;
    }

    public Map<String, Object> data() {
        return data;
    }

    public String moduleId() {
        return ScriptTestValues.requiredString(data, "module");
    }

    public Map<String, Object> args() {
        return ScriptTestValues.optionalMap(data.get("args"));
    }

    public int seed() {
        return ScriptTestValues.toInt(data.get("seed"), 0);
    }
}
