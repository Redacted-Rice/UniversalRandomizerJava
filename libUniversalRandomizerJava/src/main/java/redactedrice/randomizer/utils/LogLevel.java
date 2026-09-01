package redactedrice.randomizer.utils;

import java.util.Locale;

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

    public static LogLevel parse(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Log level cannot be blank");
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (LogLevel level : values()) {
            if (level.name().equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException(
                "Unknown log level '" + name + "'. Use DEBUG, INFO, WARN, or ERROR");
    }
}
