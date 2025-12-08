package redactedrice.randomizer.utils;

public enum LogLevel {
    DEBUG(0, "DEBUG"), INFO(1, "INFO "), WARN(2, "WARN "), ERROR(3, "ERROR");

    int level;
    String displayName;

    LogLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }
}
