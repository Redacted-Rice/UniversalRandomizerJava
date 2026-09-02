package redactedrice.randomizer.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import redactedrice.randomizer.UniversalRandomizerVersions;

/**
 * Installs the Universal Randomizer Core Lua library from this JAR's bundled {@code randomizer/}
 * manifest into a working directory.
 *
 * Reinstall is gated on UniversalRandomizerVersions CORE_VERSION via VersionedResourceInstaller.
 * Once installed for a version, later calls for the same core version are a no op. Pass
 * forceReinstall to redo it anyways.
 */
public final class RandomizerBundledResources {
    public static final String RESOURCE_ROOT = "randomizer";
    public static final String INSTALL_DIR_NAME = "randomizer";
    private static final String VERSION_MARKER_FILE_NAME = ".urc-res-ver";
    private static final String DEFAULT_BACKUPS_DIR_NAME = "backups";

    private RandomizerBundledResources() {}

    public static File install(File workingDir, boolean forceReinstall) {
        return install(workingDir, new File(workingDir, DEFAULT_BACKUPS_DIR_NAME), forceReinstall);
    }

    // backupsDir lets a host that already keeps its own backups folder (for its own bundled
    // resources) reuse it here too instead of ending up with two separate backup folders.
    public static File install(File workingDir, File backupsDir, boolean forceReinstall) {
        File targetDir = new File(workingDir, INSTALL_DIR_NAME);
        try {
            File versionMarker = new File(targetDir, VERSION_MARKER_FILE_NAME);
            VersionedResourceInstaller.installIfNeeded(RESOURCE_ROOT, targetDir, versionMarker,
                    UniversalRandomizerVersions.CORE_VERSION, backupsDir, forceReinstall);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to install randomizer Lua library", e);
        }
        return targetDir;
    }

    public static File getInstalledDir(File workingDir) {
        return new File(workingDir, INSTALL_DIR_NAME);
    }
}
