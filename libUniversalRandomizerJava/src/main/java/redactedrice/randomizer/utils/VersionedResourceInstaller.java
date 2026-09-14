package redactedrice.randomizer.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Keeps a ManifestResourceExtractor bundled resource up to date across versions without clobbering
 * local edits.
 *
 * A small marker file records which version was last installed. When the caller's current version
 * doesn't match whats recorded (or forceReinstall is set), manifest listed files get backed up and
 * replaced with the fresh bundled copy. forceReinstall only skips the version check.
 * ResourceInstallMode controls whether backup is per manifest file (UPGRADE) or the whole targetDir
 * (CLEAN_SLATE). Files under targetDir that arent part of the resource (including ones that used to
 * be but were dropped from a newer manifest) are left alone in UPGRADE mode. If the marker already
 * matches and reinstall isn't forced, this is a no op.
 */
public final class VersionedResourceInstaller {
    private VersionedResourceInstaller() {}

    // Returns true if a (re)install actually happened, false if it was already up to date.
    // Backs up into backupsDir/<targetDir's name>/... - i.e. where it's actually installed
    public static boolean installIfNeeded(String resourcePath, File targetDir, File versionMarker,
            String currentVersion, File backupsDir, boolean forceReinstall) throws IOException {
        return installIfNeeded(resourcePath, targetDir, versionMarker, currentVersion, backupsDir,
                targetDir.getName(), forceReinstall, ResourceInstallMode.UPGRADE);
    }

    // backupSubDir is where under backupsDir the backed up files land, e.g. "modules". Pass
    // null/empty to back up directly into backupsDir's root instead of a named subfolder.
    public static boolean installIfNeeded(String resourcePath, File targetDir, File versionMarker,
            String currentVersion, File backupsDir, String backupSubDir, boolean forceReinstall,
            ResourceInstallMode installMode) throws IOException {
        if (!needsReinstall(versionMarker, currentVersion, forceReinstall)) {
            return false;
        }
        backupAndInstall(resourcePath, targetDir, backupsDir, backupSubDir, installMode);
        writeVersionMarker(versionMarker, currentVersion);
        return true;
    }

    // Backs up whatever's currently on disk for resourcePath then extracts the fresh copy, with
    // no version check or marker write. Exposed for callers sharing one marker across several
    // resources, which need to gate the check once but back up and install each resource
    // individually.
    public static void backupAndInstall(String resourcePath, File targetDir, File backupsDir,
            String backupSubDir) throws IOException {
        backupAndInstall(resourcePath, targetDir, backupsDir, backupSubDir,
                ResourceInstallMode.UPGRADE);
    }

    public static void backupAndInstall(String resourcePath, File targetDir, File backupsDir,
            String backupSubDir, ResourceInstallMode installMode) throws IOException {
        File resourceBackupDir = backupSubDir == null || backupSubDir.isEmpty() ? backupsDir
                : new File(backupsDir, backupSubDir);
        if (installMode == ResourceInstallMode.CLEAN_SLATE) {
            backupDirectoryIfPresent(targetDir, resourceBackupDir);
        } else {
            for (String file : ManifestResourceExtractor.listFiles(resourcePath)) {
                backupIfPresent(new File(targetDir, file), resourceBackupDir, file);
            }
        }

        ManifestResourceExtractor.extract(resourcePath, targetDir.getAbsolutePath());
    }

    public static boolean needsReinstall(File versionMarker, String currentVersion,
            boolean forceReinstall) {
        if (forceReinstall) {
            return true;
        }
        if (!versionMarker.isFile()) {
            return true;
        }
        try {
            String installedVersion =
                    Files.readString(versionMarker.toPath(), StandardCharsets.UTF_8).trim();
            return !currentVersion.equals(installedVersion);
        } catch (IOException e) {
            // Marker's unreadable. Treat it the same as missing rather than failing startup.
            return true;
        }
    }

    public static void writeVersionMarker(File versionMarker, String currentVersion)
            throws IOException {
        Files.createDirectories(versionMarker.getParentFile().toPath());
        Files.writeString(versionMarker.toPath(), currentVersion, StandardCharsets.UTF_8);
    }

    // Moves targetDir into resourceBackupDir/<targetDir_name>.bck. No op if targetDir is missing.
    private static void backupDirectoryIfPresent(File targetDir, File resourceBackupDir)
            throws IOException {
        if (!targetDir.exists()) {
            return;
        }
        Path backupTarget = resourceBackupDir.toPath().resolve(targetDir.getName() + ".bck");
        deleteRecursively(backupTarget);
        Files.createDirectories(backupTarget.getParent());
        Files.move(targetDir.toPath(), backupTarget);
    }

    // Moves source into resourceBackupDir/<relativePath>.bck, replacing whatever backup was
    // there before. No-op if source doesn't exist. Only one generation of backup is kept.
    private static void backupIfPresent(File source, File resourceBackupDir, String relativePath)
            throws IOException {
        if (!source.exists()) {
            return;
        }
        Path backupTarget = resourceBackupDir.toPath().resolve(relativePath + ".bck");
        Files.createDirectories(backupTarget.getParent());
        deleteRecursively(backupTarget);
        Files.move(source.toPath(), backupTarget);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path p : ordered) {
                Files.delete(p);
            }
        }
    }
}
