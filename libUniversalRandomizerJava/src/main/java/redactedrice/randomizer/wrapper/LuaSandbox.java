package redactedrice.randomizer.wrapper;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import redactedrice.randomizer.logger.LuaLogFunctions;

import org.luaj.vm2.lib.DebugLib;

import java.io.File;

// sandboxed lua environment that blocks dangerous functions and libraries
public class LuaSandbox {
    Globals globals;
    String randomizerPath;

    public LuaSandbox(String randomizerPath) {
        if (randomizerPath == null || randomizerPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Randomizer path cannot be null or empty");
        }

        File randomizerDir = new File(randomizerPath);
        if (!randomizerDir.exists() || !randomizerDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Randomizer path does not exist or is not a directory: " + randomizerPath);
        }

        this.randomizerPath = new File(randomizerPath).getAbsolutePath();
        this.globals = createSandboxedGlobals();
    }

    public Globals getGlobals() {
        return globals;
    }

    private Globals createSandboxedGlobals() {
        // start with standard lua libs including the compiler
        Globals globals = JsePlatform.standardGlobals();

        // load debug lib so we can get stack traces
        globals.load(new DebugLib());

        // remove dangerous stuff
        blockDangerousFunctions(globals);

        // restrict what can be required
        setupRestrictedRequire(globals);

        // add logger functions
        setupLoggerFunctions(globals);

        return globals;
    }

    private void blockDangerousFunctions(Globals globals) {
        // remove io os and luajava completely
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);

        // restrict debug table to only have traceback
        LuaValue debugLib = globals.get("debug");
        if (!debugLib.isnil() && debugLib.istable()) {
            LuaTable restrictedDebug = new LuaTable();
            LuaValue traceback = debugLib.get("traceback");
            if (!traceback.isnil()) {
                // keep traceback so we get good error messages
                restrictedDebug.set("traceback", traceback);
            }
            globals.set("debug", restrictedDebug);
        } else {
            globals.set("debug", LuaValue.NIL);
        }

        // remove dangerous base functions
        // note loadfile is kept because require needs it for proper chunk names
        globals.set("dofile", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);
        globals.set("loadstring", LuaValue.NIL);

        // remove lua 5.1 environment manipulation
        globals.set("getfenv", LuaValue.NIL);
        globals.set("setfenv", LuaValue.NIL);
    }

    private void setupRestrictedRequire(Globals globals) {
        // configure package path to use the randomizer directory
        LuaValue packageLib = globals.get("package");
        if (!packageLib.isnil()) {
            // fix windows backslashes for lua
            String luaPath = randomizerPath.replace('\\', '/');

            // We need to get the parent directory of the randomizer path
            // to get requires to work right
            File randomizerDir = new File(randomizerPath);
            File parentDir = randomizerDir.getParentFile();
            String parentPath =
                    parentDir != null ? parentDir.getAbsolutePath().replace('\\', '/') : luaPath;

            // Set the path lua looks for modules.
            // TODO: Seems to me like we shouldn't need to specify the init but it seems like
            // we need to and I plan to revisit the loading restrictions anyways
            String packagePath = parentPath + "/?.lua;" + parentPath + "/?/init.lua";
            packageLib.set("path", LuaValue.valueOf(packagePath));

            // block c library loading
            packageLib.set("cpath", LuaValue.valueOf(""));
        }

        // wrap require to only allow randomizer modules
        final LuaValue originalRequire = globals.get("require");

        globals.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue moduleName) {
                String module = moduleName.tojstring();

                // only allow randomizer modules
                if (module.equals("randomizer") || module.startsWith("randomizer.")
                        || module.startsWith("randomizer/")) {
                    return originalRequire.call(moduleName);
                }

                // block everything else
                throw new RuntimeException(
                        "Access denied: Cannot load module '" + module + "' in sandbox");
            }
        });
    }

    public LuaValue execute(String luaCode) {
        try {
            return globals.load(luaCode).call();
        } catch (Exception e) {
            throw new RuntimeException("Lua execution error: " + e.getMessage(), e);
        }
    }

    public LuaValue executeFile(String filePath) {
        try {
            // read the file contents
            String luaCode =
                    new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)),
                            java.nio.charset.StandardCharsets.UTF_8);
            // load and run the lua code
            return globals.load(luaCode, filePath).call();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error loading Lua file '" + filePath + "': " + e.getMessage(), e);
        }
    }

    public void set(String name, LuaValue value) {
        globals.set(name, value);
    }

    public LuaValue get(String name) {
        return globals.get(name);
    }

    private void setupLoggerFunctions(Globals globals) {
        // create logger table with log functions
        LuaTable loggerTable = new LuaTable();

        loggerTable.set("debug", LuaLogFunctions.createDebugFunction());
        loggerTable.set("info", LuaLogFunctions.createInfoFunction());
        loggerTable.set("warn", LuaLogFunctions.createWarnFunction());
        loggerTable.set("error", LuaLogFunctions.createErrorFunction());
        loggerTable.set("tableToString", LuaLogFunctions.createTableToStringFunction());

        globals.set("logger", loggerTable);
    }
}
