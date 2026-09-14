package redactedrice.randomizer.utils;

// How VersionedResourceInstaller backs up an existing install before extracting fresh files.
public enum ResourceInstallMode {
    // Back up only manifest listed files. Leave everything else under targetDir alone.
    UPGRADE,
    // Move the whole targetDir into backups first. Use for dedicated install dirs when callers
    // want a clean slate, e.g. dev gradle test/run.
    CLEAN_SLATE
}
