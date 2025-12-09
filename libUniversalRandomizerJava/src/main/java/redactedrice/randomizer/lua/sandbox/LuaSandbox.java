package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import redactedrice.randomizer.lua.sandbox.monitoring.MemoryLimitExceededException;
import redactedrice.randomizer.lua.sandbox.monitoring.ResourceMonitor;
import redactedrice.randomizer.lua.sandbox.monitoring.TimeoutException;
import redactedrice.randomizer.lua.sandbox.security.SecureLuaEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// Sandboxed lua environment that blocks dangerous functions and libraries
public class LuaSandbox {
    private final SecureLuaEnvironment environment;
    private final ResourceMonitor resourceMonitor;

    public LuaSandbox(List<String> allowedRootDirectories) {
        this.environment = new SecureLuaEnvironment(allowedRootDirectories, true);
        this.resourceMonitor = new ResourceMonitor();
    }

    public Globals getGlobals() {
        return environment.getGlobals();
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    public SecureLuaEnvironment getEnvironment() {
        return environment;
    }

    // Note: Use this for dynamic code generation or to bypass path checks
    // Should only be used from the Java side and not used to execute untrusted
    // lua scripts. This still does most of the other security protections.
    public LuaValue execute(String luaCode) throws TimeoutException {
        Globals globals = environment.getGlobals();
        LuaValue chunk = globals.load(luaCode);
        return resourceMonitor.executeWithMonitoring(() -> chunk.call(), "Lua code execution");
    }

    // Note: Primiary execute for files. This will ensure they are from an
    // expected path and run with full security
    public LuaValue executeFile(String filePath) {
        // Validate that the file path is within allowed directories
        if (!environment.isPathAllowed(filePath)) {
            throw new SecurityException("Access denied: Cannot execute file '" + filePath
                    + "' - not in allowed directories");
        }
        try {
            String luaCode =
                    new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            Globals globals = environment.getGlobals();
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
        environment.getGlobals().rawset(name, value);
    }

    // Allows getting global values from Java
    public LuaValue get(String name) {
        return environment.getGlobals().get(name);
    }
}
