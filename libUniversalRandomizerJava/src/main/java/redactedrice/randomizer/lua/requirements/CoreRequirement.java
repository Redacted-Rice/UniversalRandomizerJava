package redactedrice.randomizer.lua.requirements;

public final class CoreRequirement {
    private final String key;
    private final String currentVersion;
    private final boolean isMandatory;

    public CoreRequirement(String key, String currentVersion, boolean isMandatory) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Platform requirement key cannot be null or empty");
        }
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "Platform requirement version cannot be null or empty for key '" + key + "'");
        }
        this.key = key;
        this.currentVersion = currentVersion;
        this.isMandatory = isMandatory;
    }

    public String getKey() {
        return key;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public boolean isMandatory() {
        return isMandatory;
    }
}
