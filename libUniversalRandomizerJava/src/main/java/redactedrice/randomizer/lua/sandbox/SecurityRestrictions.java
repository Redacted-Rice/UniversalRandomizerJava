package redactedrice.randomizer.lua.sandbox;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Handles security restrictions: path validation, dangerous function blocking, package protection
public class SecurityRestrictions {
    // Dangerous modules to block and not allow
    // Note Debug is handled specially as need to expose part of it in order
    // to have stack traces
    private static final Set<String> BLOCKED_MODULES =
            new HashSet<>(Arrays.asList("io", "os", "luajava"));

    private final List<Path> allowedRootDirectories;

    public SecurityRestrictions(List<Path> allowedRootDirectories) {
        this.allowedRootDirectories = allowedRootDirectories;
    }

    public boolean isPathAllowed(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            Path requestedPath = Paths.get(filePath).toAbsolutePath().normalize();
            Path resolvedPath = resolveSymlinks(requestedPath);
            return isPathWithinAllowedDirectories(resolvedPath);
        } catch (Exception e) {
            // If path resolution fails deny access
            return false;
        }
    }

    private Path resolveSymlinks(Path path) {
        try {
            return path.toRealPath();
        } catch (java.nio.file.NoSuchFileException e) {
            // File doesn't exist yet, use the normalized path
            // This is acceptable as we're validating the path, not the file content
            return path;
        } catch (Exception e) {
            return path;
        }
    }

    private boolean isPathWithinAllowedDirectories(Path resolvedPath) {
        for (Path allowedRoot : allowedRootDirectories) {
            Path normalizedAllowed = allowedRoot.toAbsolutePath().normalize();
            if (resolvedPath.startsWith(normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }

    public void blockDangerousFunctions(Globals globals) {
        removeBlockedModules(globals);
        restrictDebugLibrary(globals);
        removeDangerousBaseFunctions(globals);
        setupRestrictedSetMetatable(globals);
        setupRestrictedLoadfile(globals);
    }

    private void removeBlockedModules(Globals globals) {
        // Remove blocked modules completely
        // Note this does NOT include debug intentionally
        for (String moduleName : BLOCKED_MODULES) {
            globals.set(moduleName, LuaValue.NIL);
        }
    }

    private void restrictDebugLibrary(Globals globals) {
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
    }

    private void removeDangerousBaseFunctions(Globals globals) {
        // remove dangerous base functions
        globals.set("dofile", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);
        globals.set("loadstring", LuaValue.NIL);

        // remove lua 5.1 environment manipulation
        globals.set("getfenv", LuaValue.NIL);
        globals.set("setfenv", LuaValue.NIL);

        // Remove collectgarbage. No reason to have it and it can be abused. Probably a bit overkill
        // but it was simple to add so I decided just to do it
        globals.set("collectgarbage", LuaValue.NIL);

        // Block rawset and rawget to prevent bypassing our protections
        // I am not sure if rawget is really needed but there also isn't really
        // any reason why they should be using it so I remove it to be safe
        globals.set("rawset", LuaValue.NIL);
        globals.set("rawget", LuaValue.NIL);
    }

    private void setupRestrictedLoadfile(Globals globals) {
        // wrap loadfile to restrict access to allowed directories only
        final LuaValue originalLoadfile = globals.get("loadfile");
        globals.set("loadfile", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue filePath) {
                validateLoadfileArguments(filePath);
                String path = filePath.tojstring();
                if (!isPathAllowed(path)) {
                    throw new RuntimeException("Access denied: Cannot load file '" + path
                            + "' - not in allowed directories");
                }
                return originalLoadfile.call(filePath);
            }
        });
    }

    private void validateLoadfileArguments(LuaValue filePath) {
        // Handle bad arguments gracefully
        if (filePath.isnil()) {
            throw new IllegalArgumentException("loadfile: filename cannot be nil");
        }
        if (!filePath.isstring()) {
            throw new IllegalArgumentException(
                    "loadfile: filename must be a string, got " + filePath.typename());
        }
    }

    public void setupRestrictedRequire(Globals globals) {
        LuaValue packageLib = globals.get("package");
        if (packageLib.isnil()) {
            return;
        }

        String allowedPackagePath = buildAllowedPackagePath();
        setupPackageProtections(packageLib);
        setupProtectedPackagePath(globals, packageLib, allowedPackagePath);
        setupRequireWrapper(globals, packageLib, allowedPackagePath);
    }

    private String buildAllowedPackagePath() {
        List<String> packagePaths = new java.util.ArrayList<>();

        for (Path allowedRoot : allowedRootDirectories) {
            File allowedDir = allowedRoot.toFile();
            File parentDir = allowedDir.getParentFile();
            if (parentDir != null) {
                String parentPath = parentDir.getAbsolutePath().replace('\\', '/');
                // Standard Lua module search patterns
                packagePaths.add(parentPath + "/?.lua");
                packagePaths.add(parentPath + "/?/init.lua");
            }
        }

        return String.join(";", packagePaths);
    }

    private void setupPackageProtections(LuaValue packageLib) {
        // Protect package loaded, searchers, and preload from manipulation
        setupRestrictedPackageLoaded(packageLib);
        setupRestrictedPackageSearchers(packageLib);
        setupRestrictedPackagePreload(packageLib);
    }

    private void setupRequireWrapper(Globals globals, LuaValue packageLib,
            String allowedPackagePath) {
        // wrap require to validate that resolved paths are within allowed directories
        final LuaValue originalRequire = globals.get("require");

        globals.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue moduleName) {
                validateRequireArguments(moduleName);
                String module = moduleName.tojstring();

                // Validate module name is not blocked
                if (isBlockedModuleName(module)) {
                    throw new SecurityException(
                            "Access denied: Cannot require blocked module '" + module + "'");
                }

                validateModulePath(moduleName, module, packageLib, allowedPackagePath);

                // Path is good or couldn't be resolved
                return originalRequire.call(moduleName);
            }
        });
    }

    private void validateRequireArguments(LuaValue moduleName) {
        // Handle bad arguments gracefully
        if (moduleName.isnil()) {
            throw new IllegalArgumentException("require: module name cannot be nil");
        }
        if (!moduleName.isstring()) {
            throw new IllegalArgumentException(
                    "require: module name must be a string, got " + moduleName.typename());
        }
    }

    private void validateModulePath(LuaValue moduleName, String module, LuaValue packageLib,
            String allowedPackagePath) {
        try {
            // Use package.searchpath to find where the module would be loaded from
            if (!packageLib.isnil()) {
                LuaValue searchpath = packageLib.get("searchpath");
                if (!searchpath.isnil() && searchpath.isfunction()) {
                    LuaValue resolvedPath =
                            searchpath.call(moduleName, LuaValue.valueOf(allowedPackagePath));
                    if (resolvedPath.isstring()) {
                        String filePath = resolvedPath.tojstring();
                        if (!isPathAllowed(filePath)) {
                            throw new RuntimeException("Access denied: Cannot load module '"
                                    + module + "' - resolved path '" + filePath
                                    + "' is not in allowed directories");
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Not found - continue on and it will give the not found error
        }
    }

    private boolean isBlockedModuleName(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return false;
        }

        String normalized = moduleName.trim();

        // Is the module blocked?
        if (BLOCKED_MODULES.contains(normalized)) {
            return true;
        }

        // Block debug even though it's restricted rather than completely removed
        // to prevent loading the full debug module via require
        if (normalized.equals("debug")) {
            return true;
        }

        return false;
    }

    private LuaTable createReadOnlyProtectedTable(LuaTable original, String tableName) {
        return new ReadOnlyLuaTable(original, tableName);
    }

    private LuaTable createModuleFilteredProtectedTable(LuaTable original) {
        return new ModuleFilteredLuaTable(original, BLOCKED_MODULES);
    }

    private void setupRestrictedPackageLoaded(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        LuaValue loaded = packageLib.get("loaded");
        if (loaded.isnil() || !loaded.istable()) {
            return;
        }

        final LuaTable originalLoaded = (LuaTable) loaded;
        packageLib.set("loaded", createModuleFilteredProtectedTable(originalLoaded));
    }

    private void setupRestrictedPackageSearchers(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        // <= Lua 5.1 uses loaders, >= 5.2 uses searchers so we need to
        // block both for compatibility

        LuaValue loaders = packageLib.get("loaders");
        if (!loaders.isnil() && loaders.istable()) {
            packageLib.set("loaders", createReadOnlyProtectedTable((LuaTable) loaders, "loaders"));
        }

        LuaValue searchers = packageLib.get("searchers");
        if (!searchers.isnil() && searchers.istable()) {
            packageLib.set("searchers",
                    createReadOnlyProtectedTable((LuaTable) searchers, "searchers"));
        }
    }

    private void setupRestrictedPackagePreload(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        LuaValue preload = packageLib.get("preload");
        if (!preload.isnil() && preload.istable()) {
            packageLib.set("preload", createReadOnlyProtectedTable((LuaTable) preload, "preload"));
        }
    }

    private void setupProtectedPackagePath(Globals globals, LuaValue packageLib,
            String allowedPackagePath) {
        if (packageLib.isnil()) {
            return;
        }
        LuaTable packageTable = (LuaTable) packageLib;

        // set the packages to the allowed paths
        packageTable.rawset("path", LuaValue.valueOf(allowedPackagePath));
        // block c library loading completely
        packageTable.rawset("cpath", LuaValue.valueOf(""));

        // Create a wrapper table that blocks changes to path and cpath
        LuaTable protectedPackage = createProtectedPackageTable(packageTable);
        globals.set("package", protectedPackage);
    }

    private LuaTable createProtectedPackageTable(LuaTable packageTable) {
        return new DelegatingLuaTable(packageTable) {
            @Override
            public void rawset(LuaValue key, LuaValue value) {
                // Block changes to path and cpath fields
                if (key.isstring()) {
                    String keyStr = key.tojstring();
                    if (keyStr.equals("path") || keyStr.equals("cpath")) {
                        throw new SecurityException("Cannot modify package." + keyStr);
                    }
                }
                original.rawset(key, value);
            }

            // Also override normal set. Just delegate to rawset for simplicity
            @Override
            public void set(LuaValue key, LuaValue value) {
                rawset(key, value);
            }
        };
    }

    private void setupRestrictedSetMetatable(Globals globals) {
        // Block global and restricted functions metatables from being modified
        final LuaValue originalSetmetatable = globals.get("setmetatable");
        final Globals globalsFinal = globals;
        globals.set("setmetatable", createSetmetatableWrapper(originalSetmetatable, globalsFinal));
    }

    private TwoArgFunction createSetmetatableWrapper(LuaValue originalSetmetatable,
            Globals globals) {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue metatable) {
                validateSetmetatableArguments(table, metatable);
                validateSetmetatableTarget(table, globals);
                // Allow setmetatable for all other tables
                return originalSetmetatable.call(table, metatable);
            }
        };
    }

    private void validateSetmetatableArguments(LuaValue table, LuaValue metatable) {
        // Handle bad arguments gracefully
        if (table.isnil()) {
            throw new IllegalArgumentException("setmetatable: table cannot be nil");
        }
        if (!table.istable()) {
            throw new IllegalArgumentException(
                    "setmetatable: first argument must be a table, got " + table.typename());
        }
        if (!metatable.isnil() && !metatable.istable()) {
            throw new IllegalArgumentException(
                    "setmetatable: metatable must be nil or a table, got " + metatable.typename());
        }
    }

    private void validateSetmetatableTarget(LuaValue table, Globals globals) {
        // Prevent modification of globals table metatable
        if (table == globals || (table.istable() && table.touserdata() == globals)) {
            throw new SecurityException("Cannot modify metatable of global environment");
        }

        // Prevent modification of package system tables metatables
        validatePackageTableMetatable(table, globals);
    }

    private void validatePackageTableMetatable(LuaValue table, Globals globals) {
        LuaValue packageLib = globals.get("package");
        if (packageLib.isnil() || !packageLib.istable()) {
            return;
        }

        LuaTable packageTable = (LuaTable) packageLib;
        String[] protectedPackageTables = {"loaded", "loaders", "searchers", "preload"};

        for (String tableName : protectedPackageTables) {
            LuaValue protectedTable = packageTable.get(tableName);
            if (!protectedTable.isnil()
                    && (table == protectedTable || table.equals(protectedTable))) {
                throw new SecurityException(
                        "Cannot modify metatable of protected " + tableName + " table");
            }
        }
    }

    public void setupProtectedGlobals(Globals globals) {
        // Create a metatable that prevents new global assignments
        LuaTable globalsMetatable = new LuaTable();

        globalsMetatable.set(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            // __newindex is only called for keys that don't exist in the table
            // so we can just block it to prevent new assignments
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                // Not sure its possible to pass nil here but guard for it just in case
                String keyStr = key.isnil() ? "nil" : key.tojstring();
                throw new SecurityException("Cannot create new global variable '" + keyStr
                        + "'. Global environment is protected. Use local variables instead.");
            }
        });
        // Don't modify __index so we can still access existing globals

        globals.setmetatable(globalsMetatable);
    }
}
