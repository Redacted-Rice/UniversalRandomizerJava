package redactedrice.randomizer.lua.sandbox.security;

import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import redactedrice.randomizer.lua.sandbox.LuaLogFunctions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// Secure Lua environment that uses the security policies
public class SecureLuaEnvironment {
    private final Globals globals;
    private final FileSystemPolicy fileSystemPolicy;
    private final PackagePolicy packagePolicy;

    public SecureLuaEnvironment(List<String> allowedRootDirectories, boolean includeLogger) {
        // Setup policies to apply during setup
        BaseFunctionsPolicy baseFunctionsPolicy = new BaseFunctionsPolicy();
        MetatablePolicy metatablePolicy = new MetatablePolicy();
        GlobalsPolicy globalsPolicy = new GlobalsPolicy();

        // runtime policies that need to be held on to
        FileSystemPolicy fileSystemPolicy = new FileSystemPolicy(allowedRootDirectories);
        PackagePolicy packagePolicy =
                new PackagePolicy(fileSystemPolicy, SandboxModulePolicy.blockedModulesForRequire());

        // Setup globals
        Globals globals = JsePlatform.standardGlobals();
        globals.load(new DebugLib());

        // Apply setup policies
        baseFunctionsPolicy.applyToGlobals(globals);
        metatablePolicy.applyToGlobals(globals);

        // Apply runtime policies as appropriate at this stage
        fileSystemPolicy.applyToGlobals(globals);
        packagePolicy.applyToGlobals(globals);

        // Add logger if requested
        if (includeLogger) {
            setupLoggerFunctions(globals);
        }

        // Apply (setup policy) GlobalsPolicy last
        globalsPolicy.applyToGlobals(globals);

        // Store the ones we need to hold onto
        this.globals = globals;
        this.fileSystemPolicy = fileSystemPolicy;
        this.packagePolicy = packagePolicy;
    }

    public Globals getGlobals() {
        return globals;
    }

    public boolean isPathAllowed(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            return fileSystemPolicy.isPathAllowed(path);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isModuleAllowed(String moduleName) {
        return packagePolicy.isModuleAllowed(moduleName);
    }

    private void setupLoggerFunctions(Globals globals) {
        globals.set("logger", LuaLogFunctions.createLoggerTable());
    }
}
