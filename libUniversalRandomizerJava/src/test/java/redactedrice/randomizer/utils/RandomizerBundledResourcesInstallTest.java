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

class RandomizerBundledResourcesInstallTest {
    @TempDir
    Path tempDir;

    @Test
    void upgradeModeKeepsNonManifestFilesOnForcedReinstall() throws IOException {
        File workingDir = tempDir.toFile();
        File randomizerDir = RandomizerBundledResources.install(workingDir, false);
        Path orphan = randomizerDir.toPath().resolve("orphan.lua");
        Files.writeString(orphan, "-- stay", StandardCharsets.UTF_8);

        RandomizerBundledResources.install(workingDir, backupsDir(), true,
                ResourceInstallMode.UPGRADE);

        assertTrue(Files.exists(orphan));
        assertEquals("-- stay", Files.readString(orphan, StandardCharsets.UTF_8));
    }

    @Test
    void cleanSlateModeRemovesNonManifestFilesOnForcedReinstall() throws IOException {
        File workingDir = tempDir.toFile();
        File randomizerDir = RandomizerBundledResources.install(workingDir, false);
        Path orphan = randomizerDir.toPath().resolve("orphan.lua");
        Files.writeString(orphan, "-- go away", StandardCharsets.UTF_8);

        RandomizerBundledResources.install(workingDir, backupsDir(), true,
                ResourceInstallMode.CLEAN_SLATE);

        assertFalse(Files.exists(orphan));
        Path backupOrphan = backupsDir().toPath().resolve("randomizer/randomizer.bck/orphan.lua");
        assertTrue(Files.exists(backupOrphan));
        assertEquals("-- go away", Files.readString(backupOrphan, StandardCharsets.UTF_8));
    }

    @Test
    void installIsNoOpWhenVersionMarkerAlreadyMatches() throws IOException {
        File workingDir = tempDir.toFile();
        RandomizerBundledResources.install(workingDir, false);
        Path initLua = RandomizerBundledResources.getInstalledDir(workingDir).toPath()
                .resolve("init.lua");
        Files.writeString(initLua, "edited", StandardCharsets.UTF_8);

        RandomizerBundledResources.install(workingDir, false);

        assertEquals("edited", Files.readString(initLua, StandardCharsets.UTF_8));
    }

    private File backupsDir() {
        return tempDir.resolve("backups").toFile();
    }
}
