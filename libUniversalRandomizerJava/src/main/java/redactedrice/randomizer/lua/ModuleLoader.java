package redactedrice.randomizer.lua;

import redactedrice.randomizer.lua.sandbox.LuaSandbox;
import redactedrice.randomizer.utils.IssueTracker;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Handles loading Lua module files and executing them
// Just loads and executes - does not parse or validate
public class ModuleLoader {
    private final LuaSandbox sandbox;

    public ModuleLoader(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
    }

    // Load a single module file and return the Lua table result
    public LuaValue loadFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        try {
            LuaValue result = sandbox.executeFile(file.getAbsolutePath());
            if (!result.istable()) {
                IssueTracker.addError(
                        file.getName() + " did not return a table (got " + result.typename() + ")");
                return null;
            }
            return result;
        } catch (LuaError e) {
            IssueTracker.addError("Lua error in " + file.getName() + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            IssueTracker.addError("Error loading " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    // Find all .lua files in a directory recursively
    public List<Path> findLuaFiles(Path directory) {
        if (directory == null) {
            return new ArrayList<>();
        }

        File dir = directory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }

        return findLuaFilesRecursive(dir);
    }

    private List<Path> findLuaFilesRecursive(File directory) {
        List<Path> luaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // recurse into subdirectories
                    luaFiles.addAll(findLuaFilesRecursive(file));
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".lua")) {
                    luaFiles.add(file.toPath());
                }
            }
        }

        return luaFiles;
    }
}
