package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import redactedrice.randomizer.lua.sandbox.security.BaseFunctionsPolicy;
import redactedrice.randomizer.utils.LuaJavaConverter;

// One case from a Lua file. A file may return this table, or an array of them.
public final class ScriptTestCase {
    private final Path source;
    private final int index;
    private final Map<String, Object> data;

    private ScriptTestCase(Path source, int index, Map<String, Object> data) {
        this.source = source;
        this.index = index;
        this.data = data;
    }

    public static ScriptTestCase load(Path caseFile) throws IOException {
        List<ScriptTestCase> cases = loadAll(caseFile);
        if (cases.size() != 1) {
            throw new IllegalArgumentException(
                    caseFile + " has " + cases.size() + " cases. Use loadAll for multi case files");
        }
        return cases.get(0);
    }

    @SuppressWarnings("unchecked")
    public static List<ScriptTestCase> loadAll(Path caseFile) throws IOException {
        String lua = Files.readString(caseFile, StandardCharsets.UTF_8);
        Globals globals = JsePlatform.standardGlobals();
        // Apply similar restrictions as to the real sandbox
        new BaseFunctionsPolicy().applyToGlobals(globals);
        allowRequiresFrom(globals, caseFile.getParent());
        LuaValue chunk = globals.load(lua, caseFile.getFileName().toString());
        Object loaded = LuaJavaConverter.luaToJava(chunk.call());

        List<Map<String, Object>> tables = new ArrayList<>();
        if (loaded instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new IllegalArgumentException(caseFile + " returned an empty list of cases");
            }
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException(
                            caseFile + " array entries must be case tables");
                }
                tables.add((Map<String, Object>) map);
            }
        } else if (loaded instanceof Map<?, ?> map) {
            tables.add((Map<String, Object>) map);
        } else {
            throw new IllegalArgumentException(
                    caseFile + " must return a case table or an array of case tables");
        }

        List<ScriptTestCase> cases = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            cases.add(new ScriptTestCase(caseFile, i + 1, tables.get(i)));
        }
        return cases;
    }

    // So case files can require helpers next to them, like shared/card_sets.lua
    private static void allowRequiresFrom(Globals globals, Path testsDir) {
        if (testsDir == null) {
            return;
        }
        String dir = testsDir.toAbsolutePath().toString().replace('\\', '/');
        LuaValue pkg = globals.get("package");
        if (pkg.isnil()) {
            return;
        }
        String existing = pkg.get("path").optjstring("");
        pkg.set("path", dir + "/?.lua;" + dir + "/?/init.lua;" + existing);
    }

    public Path source() {
        return source;
    }

    public int index() {
        return index;
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

    public String displayName() {
        String caseName = ScriptTestValues.optionalString(data, "name");
        if (caseName == null) {
            caseName = String.valueOf(index);
        }
        return source.getFileName() + " / " + caseName;
    }
}
