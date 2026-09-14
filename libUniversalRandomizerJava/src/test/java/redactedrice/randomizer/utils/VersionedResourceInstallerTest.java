package redactedrice.randomizer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionedResourceInstallerTest {
    private static final String TEST_RESOURCE = "install-test-resource";
    private static final String VERSION = "test-1.0";

    @TempDir
    Path tempDir;

    @Test
    void needsReinstallWhenForceReinstallIsSet() throws IOException {
        File marker = writeMarker("test-1.0");
        assertTrue(VersionedResourceInstaller.needsReinstall(marker, VERSION, true));
    }

    @Test
    void needsReinstallWhenMarkerIsMissing() {
        File marker = tempDir.resolve("missing-marker").toFile();
        assertTrue(VersionedResourceInstaller.needsReinstall(marker, VERSION, false));
    }

    @Test
    void needsReinstallWhenVersionDiffers() throws IOException {
        File marker = writeMarker("older");
        assertTrue(VersionedResourceInstaller.needsReinstall(marker, VERSION, false));
    }

    @Test
    void needsReinstallWhenVersionMatches() throws IOException {
        File marker = writeMarker(VERSION);
        assertFalse(VersionedResourceInstaller.needsReinstall(marker, VERSION, false));
    }

    @Test
    void needsReinstallWhenMarkerIsUnreadable() throws IOException {
        Path markerPath = tempDir.resolve("broken-marker");
        Files.createDirectories(markerPath);
        assertTrue(
                VersionedResourceInstaller.needsReinstall(markerPath.toFile(), VERSION, false));
    }

    @Test
    void installIfNeededSkipsWhenAlreadyUpToDate() throws IOException {
        File targetDir = tempDir.resolve("payload").toFile();
        File marker = tempDir.resolve("ver").toFile();
        VersionedResourceInstaller.writeVersionMarker(marker, VERSION);

        assertFalse(VersionedResourceInstaller.installIfNeeded(TEST_RESOURCE, targetDir, marker,
                VERSION, backupsDir(), false));
        assertFalse(new File(targetDir, "bundled.txt").exists());
    }

    @Test
    void installIfNeededRunsWhenForceReinstallIsSet() throws IOException {
        File targetDir = tempDir.resolve("payload").toFile();
        File marker = tempDir.resolve("ver").toFile();
        VersionedResourceInstaller.writeVersionMarker(marker, VERSION);

        assertTrue(VersionedResourceInstaller.installIfNeeded(TEST_RESOURCE, targetDir, marker,
                VERSION, backupsDir(), true));
        assertEquals("from-classpath", readFile(targetDir.toPath().resolve("bundled.txt")));
    }

    @Test
    void upgradeModeBacksUpManifestFilesOnly() throws IOException {
        File targetDir = tempDir.resolve("payload").toFile();
        writeFile(targetDir.toPath().resolve("bundled.txt"), "user-edited");
        writeFile(targetDir.toPath().resolve("orphan.txt"), "keep-me");

        VersionedResourceInstaller.backupAndInstall(TEST_RESOURCE, targetDir, backupsDir(),
                targetDir.getName(), ResourceInstallMode.UPGRADE);

        assertEquals("from-classpath", readFile(targetDir.toPath().resolve("bundled.txt")));
        assertEquals("keep-me", readFile(targetDir.toPath().resolve("orphan.txt")));
        assertEquals("user-edited", readFile(backupFile("payload/bundled.txt.bck")));
        assertFalse(Files.exists(backupsDir().toPath().resolve("payload/payload.bck")));
    }

    @Test
    void cleanSlateModeMovesWholeTargetDirToBackup() throws IOException {
        File targetDir = tempDir.resolve("payload").toFile();
        writeFile(targetDir.toPath().resolve("bundled.txt"), "user-edited");
        writeFile(targetDir.toPath().resolve("orphan.txt"), "remove-me");

        VersionedResourceInstaller.backupAndInstall(TEST_RESOURCE, targetDir, backupsDir(),
                targetDir.getName(), ResourceInstallMode.CLEAN_SLATE);

        assertEquals("from-classpath", readFile(targetDir.toPath().resolve("bundled.txt")));
        assertFalse(Files.exists(targetDir.toPath().resolve("orphan.txt")));
        assertEquals("user-edited",
                readFile(backupsDir().toPath().resolve("payload/payload.bck/bundled.txt")));
        assertEquals("remove-me",
                readFile(backupsDir().toPath().resolve("payload/payload.bck/orphan.txt")));
    }

    @Test
    void forceReinstallDoesNotImplyCleanSlate() throws IOException {
        File targetDir = tempDir.resolve("payload").toFile();
        File marker = tempDir.resolve("ver").toFile();
        writeFile(targetDir.toPath().resolve("bundled.txt"), "user-edited");
        writeFile(targetDir.toPath().resolve("orphan.txt"), "keep-me");
        VersionedResourceInstaller.writeVersionMarker(marker, VERSION);

        assertTrue(VersionedResourceInstaller.installIfNeeded(TEST_RESOURCE, targetDir, marker,
                VERSION, backupsDir(), targetDir.getName(), true, ResourceInstallMode.UPGRADE));

        assertEquals("from-classpath", readFile(targetDir.toPath().resolve("bundled.txt")));
        assertEquals("keep-me", readFile(targetDir.toPath().resolve("orphan.txt")));
        assertEquals("user-edited", readFile(backupFile("payload/bundled.txt.bck")));
    }

    private File backupsDir() {
        return tempDir.resolve("backups").toFile();
    }

    private File writeMarker(String version) throws IOException {
        File marker = tempDir.resolve("marker").toFile();
        VersionedResourceInstaller.writeVersionMarker(marker, version);
        return marker;
    }

    private Path backupFile(String relativePath) {
        return backupsDir().toPath().resolve(relativePath);
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }
}
