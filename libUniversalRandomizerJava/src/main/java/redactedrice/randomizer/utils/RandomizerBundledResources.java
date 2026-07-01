package redactedrice.randomizer.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Installs the Universal Randomizer Core Lua library from this JAR's bundled
 * {@code randomizer/} manifest into a working directory.
 */
public final class RandomizerBundledResources {
    public static final String RESOURCE_ROOT = "randomizer";
    public static final String INSTALL_DIR_NAME = "randomizer";

    private RandomizerBundledResources() {
    }

    public static File install(File workingDir, boolean overwriteExisting) {
        File targetDir = new File(workingDir, INSTALL_DIR_NAME);
        try {
            ManifestResourceExtractor.extract(RESOURCE_ROOT, targetDir.getAbsolutePath(),
                    overwriteExisting);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to install randomizer Lua library", e);
        }
        return targetDir;
    }

    public static File getInstalledDir(File workingDir) {
        return new File(workingDir, INSTALL_DIR_NAME);
    }
}
