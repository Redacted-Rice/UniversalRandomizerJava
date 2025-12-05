package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.LuaTable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Sandboxed lua environment that blocks dangerous functions and libraries
public class LuaSandbox {
    private final Globals globals;
    private final List<Path> allowedRootDirectories;
    private final ResourceMonitor resourceMonitor;
    private final SecurityRestrictions securityRestrictions;

    public LuaSandbox(List<String> allowedRootDirectories) {
        if (allowedRootDirectories == null || allowedRootDirectories.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one allowed root directory must be provided");
        }

        this.allowedRootDirectories = new ArrayList<>();
        for (String dirPath : allowedRootDirectories) {
            if (dirPath == null || dirPath.trim().isEmpty()) {
                continue;
            }

            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new IllegalArgumentException(
                        "Allowed directory does not exist or is not a directory: " + dirPath);
            }

            this.allowedRootDirectories.add(Paths.get(dir.getAbsolutePath()).normalize());
        }

        if (this.allowedRootDirectories.isEmpty()) {
            throw new IllegalArgumentException("No valid allowed root directories provided");
        }

        this.resourceMonitor = new ResourceMonitor();
        this.securityRestrictions = new SecurityRestrictions(this.allowedRootDirectories);
        this.globals = createSandboxedGlobals();
    }

    public Globals getGlobals() {
        return globals;
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    public SecurityRestrictions getSecurityRestrictions() {
        return securityRestrictions;
    }

    private Globals createSandboxedGlobals() {
        // start with standard lua libs including the compiler
        Globals globals = JsePlatform.standardGlobals();

        // load debug lib so we can get stack traces
        globals.load(new DebugLib());

        // remove dangerous stuff
        securityRestrictions.blockDangerousFunctions(globals);

        // restrict what can be required
        securityRestrictions.setupRestrictedRequire(globals);

        // add logger functions
        setupLoggerFunctions(globals);

        // protect globals from modification
        // Note this must be done last or else we will block ourself
        securityRestrictions.setupProtectedGlobals(globals);

        return globals;
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

    // Note: Use this for dynamic code generation or to bypass path checks
    // Should only be used from the Java side and not used to execute untrusted
    // lua scripts. This still does most of the other security protections.
    public LuaValue execute(String luaCode) throws TimeoutException {
        // Load and parse the code then execute so the loading is not counted against the timeout
        LuaValue chunk = globals.load(luaCode);
        return resourceMonitor.executeWithMonitoring(() -> chunk.call(), "Lua code execution");
    }

    // Note: Primiary execute for files. This will ensure they are from an
    // expected path and run with full security
    public LuaValue executeFile(String filePath) {
        // Validate that the file path is within allowed directories
        if (!securityRestrictions.isPathAllowed(filePath)) {
            throw new SecurityException("Access denied: Cannot execute file '" + filePath
                    + "' - not in allowed directories");
        }
        try {
            // read the file contents
            String luaCode =
                    new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            // Load and parse the code then execute so the loading is not counted against the
            // timeout
            LuaValue chunk = globals.load(luaCode, filePath);
            return resourceMonitor.executeWithMonitoring(() -> chunk.call(),
                    "Lua file execution: " + filePath);
        } catch (SecurityException e) {
            throw e;
        } catch (MemoryLimitExceededException e) {
            throw e;
        } catch (TimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error loading Lua file '" + filePath + "': " + e.getMessage(), e);
        }
    }

    // Allows setting global values from Java
    public void set(String name, LuaValue value) {
        // Use rawset to bypass the __newindex metamethod protection
        // This allows us to set globals without triggering the security protection meant for Lua
        // scripts
        globals.rawset(name, value);
    }

    // Allows getting global values from Java
    public LuaValue get(String name) {
        return globals.get(name);
    }
}
