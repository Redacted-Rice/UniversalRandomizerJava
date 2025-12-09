package redactedrice.randomizer.lua.sandbox.security;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

// Restricts the Lua package system
public class PackagePolicy {

    private final FileSystemPolicy fileSystemPolicy;
    private final Set<String> blockedModules;

    public PackagePolicy(FileSystemPolicy fileSystemPolicy, final Set<String> blockedModules) {
        if (fileSystemPolicy == null) {
            throw new IllegalArgumentException("FileSystemPolicy cannot be null");
        }
        this.blockedModules = blockedModules;
        this.fileSystemPolicy = fileSystemPolicy;
    }

    public boolean isModuleAllowed(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return false;
        }
        return !blockedModules.contains(moduleName.trim());
    }

    public void applyToGlobals(Globals globals) {
        setupRestrictedRequire(globals);
    }

    private void setupRestrictedRequire(Globals globals) {
        LuaValue packageLib = globals.get("package");
        if (packageLib.isnil()) {
            return;
        }

        String allowedPackagePath = fileSystemPolicy.buildAllowedPackagePath();
        setupPackageProtections(packageLib);
        setupProtectedPackagePath(globals, packageLib, allowedPackagePath);
        setupRequireWrapper(globals, packageLib, allowedPackagePath);
    }

    private void setupPackageProtections(LuaValue packageLib) {
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
                if (!isModuleAllowed(module)) {
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
                        Path pathObj = Paths.get(filePath);
                        if (!fileSystemPolicy.isPathAllowed(pathObj)) {
                            throw new SecurityException("Access denied: Cannot load module '"
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

    private void setupRestrictedPackageLoaded(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        LuaValue loaded = packageLib.get("loaded");
        if (loaded.isnil() || !loaded.istable()) {
            return;
        }

        final LuaTable originalLoaded = (LuaTable) loaded;
        packageLib.set("loaded", new ModuleFilteredLuaTable(originalLoaded, blockedModules));
    }

    private void setupRestrictedPackageSearchers(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        // <= Lua 5.1 uses loaders, >= 5.2 uses searchers so we need to
        // block both for compatibility

        LuaValue loaders = packageLib.get("loaders");
        if (!loaders.isnil() && loaders.istable()) {
            packageLib.set("loaders", new ReadOnlyLuaTable((LuaTable) loaders, "loaders"));
        }

        LuaValue searchers = packageLib.get("searchers");
        if (!searchers.isnil() && searchers.istable()) {
            packageLib.set("searchers", new ReadOnlyLuaTable((LuaTable) searchers, "searchers"));
        }
    }

    private void setupRestrictedPackagePreload(LuaValue packageLib) {
        if (packageLib.isnil()) {
            return;
        }

        LuaValue preload = packageLib.get("preload");
        if (!preload.isnil() && preload.istable()) {
            packageLib.set("preload", new ReadOnlyLuaTable((LuaTable) preload, "preload"));
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
}
