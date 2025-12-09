package redactedrice.randomizer.lua.sandbox.security;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Restricts file system access to allowed directories only
public class FileSystemPolicy {
    private final List<Path> allowedRootDirectories;

    public FileSystemPolicy(List<String> allowedRootDirectories) {
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
    }

    public boolean isPathAllowed(Path filePath) {
        if (filePath == null) {
            return false;
        }

        try {
            Path requestedPath = filePath.toAbsolutePath().normalize();
            Path resolvedPath = resolveSymlinks(requestedPath);
            return isPathWithinAllowedDirectories(resolvedPath);
        } catch (Exception e) {
            // If path resolution fails deny access
            return false;
        }
    }

    public void applyToGlobals(Globals globals) {
        setupRestrictedLoadfile(globals);
    }

    public List<Path> getAllowedRootDirectories() {
        return new ArrayList<>(allowedRootDirectories);
    }

    public String buildAllowedPackagePath() {
        List<String> packagePaths = new ArrayList<>();

        for (Path allowedRoot : allowedRootDirectories) {
            File allowedDir = allowedRoot.toFile();
            File parentDir = allowedDir.getParentFile();
            if (parentDir != null) {
                String parentPath = parentDir.getAbsolutePath().replace('\\', '/');
                packagePaths.add(parentPath + "/?.lua");
                packagePaths.add(parentPath + "/?/init.lua");
            }
        }

        return String.join(";", packagePaths);
    }

    private Path resolveSymlinks(Path path) {
        try {
            return path.toRealPath();
        } catch (NoSuchFileException e) {
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

    private void setupRestrictedLoadfile(Globals globals) {
        // wrap loadfile to restrict access to allowed directories only
        final LuaValue originalLoadfile = globals.get("loadfile");
        globals.set("loadfile", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue filePath) {
                validateLoadfileArguments(filePath);
                String path = filePath.tojstring();
                Path pathObj = Paths.get(path);
                if (!isPathAllowed(pathObj)) {
                    throw new SecurityException("Access denied: Cannot load file '" + path
                            + "' - not in allowed directories");
                }
                return originalLoadfile.call(filePath);
            }
        });
    }

    private void validateLoadfileArguments(LuaValue filePath) {
        if (filePath.isnil()) {
            throw new IllegalArgumentException("loadfile: filename cannot be nil");
        }
        if (!filePath.isstring()) {
            throw new IllegalArgumentException(
                    "loadfile: filename must be a string, got " + filePath.typename());
        }
    }
}
