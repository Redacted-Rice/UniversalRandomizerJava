package redactedrice.randomizer.logger;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// logging utility that can be called from both java and lua
// supports multiple output streams per log level
public class Logger {
    private static final String DEFAULT_FORMAT_STRING = "%TIMESTAMP [%LEVEL] %MODULE: %MESSAGE";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "HH:mm:ss.SSS";
    // -1 means no truncating
    private static final int DEFAULT_MAX_MODULE_NAME_LENGTH = -1;
    private static final boolean DEFAULT_FORCE_MODULE_WIDTH = false;

    private static boolean enabled = true;
    private static boolean showTimestamp = false;
    private static boolean showModuleName = false;
    private static String currentModuleName = null;
    private static LogLevel minLogLevel = LogLevel.DEBUG;
    private static String formatString = DEFAULT_FORMAT_STRING;
    private static int maxModuleNameLength = DEFAULT_MAX_MODULE_NAME_LENGTH;
    private static boolean forceModuleWidth = DEFAULT_FORCE_MODULE_WIDTH;
    private static String timestampFormat = DEFAULT_TIMESTAMP_FORMAT;

    // Map of log levels to their output streams
    private static Map<LogLevel, List<OutputStream>> levelStreams = new HashMap<>();

    static {
        // initialize default streams for each log level
        // by default, each level has its own list of output streams
        for (LogLevel level : LogLevel.values()) {
            levelStreams.put(level, new ArrayList<>());
            // errors go to stderr, everything else to stdout by default
            if (level == LogLevel.ERROR) {
                levelStreams.get(level).add(System.err);
            } else {
                levelStreams.get(level).add(System.out);
            }
        }
    }

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

    public static void addStreamForLevel(LogLevel level, OutputStream stream) {
        if (level == null || stream == null) {
            return; // silently ignore null parameters to avoid errors
        }
        List<OutputStream> streams = levelStreams.get(level);
        // only add if we dont already have this stream to avoid duplicates
        if (streams != null && !streams.contains(stream)) {
            streams.add(stream);
        }
    }

    public static void addStreamForLevels(OutputStream stream, LogLevel... levels) {
        if (stream == null || levels == null || levels.length == 0) {
            return; // silently ignore invalid parameters to avoid errors
        }
        // add stream to multiple levels at once
        for (LogLevel level : levels) {
            if (level != null) {
                List<OutputStream> streams = levelStreams.get(level);
                if (streams != null && !streams.contains(stream)) {
                    streams.add(stream);
                }
            }
        }
    }

    public static void addStreamForAllLevels(OutputStream stream) {
        if (stream == null) {
            return; // silently ignore null parameters to avoid errors
        }
        // add stream to all log levels
        for (LogLevel level : LogLevel.values()) {
            List<OutputStream> streams = levelStreams.get(level);
            if (streams != null && !streams.contains(stream)) {
                streams.add(stream);
            }
        }
    }

    public static void removeAllStreamsForLevel(LogLevel level) {
        if (level == null) {
            return; // silently ignore null parameters to avoid errors
        }
        List<OutputStream> streams = levelStreams.get(level);
        if (streams != null) {
            // always ensure system.out/err remain after clearing
            // this prevents accidental loss of console output
            OutputStream defaultStream = (level == LogLevel.ERROR) ? System.err : System.out;
            streams.clear();
            streams.add(defaultStream);
        }
    }

    public static void clearAllStreams() {
        // clear all custom streams but keep default console outputs
        for (LogLevel level : LogLevel.values()) {
            List<OutputStream> streams = levelStreams.get(level);
            if (streams != null) {
                // always ensure system.out/err remain after clearing
                OutputStream defaultStream = (level == LogLevel.ERROR) ? System.err : System.out;
                streams.clear();
                streams.add(defaultStream);
            }
        }
    }

    public static void setEnabled(boolean enabled) {
        Logger.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setShowTimestamp(boolean show) {
        showTimestamp = show;
    }

    public static void setTimestampFormat(String format) {
        if (format == null || format.isEmpty()) {
            timestampFormat = DEFAULT_TIMESTAMP_FORMAT;
            return;
        }

        // Validate the format string by trying to create a SimpleDateFormat
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            // Test the format by formatting a date
            sdf.format(new Date());
            timestampFormat = format;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timestamp format string: " + format, e);
        }
    }

    public static String getTimestampFormat() {
        return timestampFormat;
    }

    public static void setShowModuleName(boolean show) {
        showModuleName = show;
    }

    public static void setMaxModuleNameLength(int maxLength) {
        if (maxLength == -1 || maxLength > 0) {
            maxModuleNameLength = maxLength;
        }
    }

    public static int getMaxModuleNameLength() {
        return maxModuleNameLength;
    }

    public static void setForceModuleWidth(boolean force) {
        forceModuleWidth = force;
    }

    public static boolean isForceModuleWidth() {
        return forceModuleWidth;
    }

    public static void setCurrentModuleName(String moduleName) {
        currentModuleName = moduleName;
    }

    public static String getCurrentModuleName() {
        return currentModuleName;
    }

    public static void setMinLogLevel(LogLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("LogLevel cannot be null");
        }
        minLogLevel = level;
    }

    public static LogLevel getMinLogLevel() {
        return minLogLevel;
    }

    public static void setFormatString(String format) {
        if (format != null && !format.isEmpty()) {
            formatString = format;
        } else {
            formatString = DEFAULT_FORMAT_STRING;
        }
    }

    public static String getFormatString() {
        return formatString;
    }

    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public static void log(LogLevel level, String message) {
        if (!enabled || level.getLevel() < minLogLevel.getLevel()) {
            return;
        }

        String formatted = formatMessage(level, message);
        List<OutputStream> streams = levelStreams.get(level);

        if (streams != null && !streams.isEmpty()) {
            byte[] bytes = (formatted + System.lineSeparator()).getBytes();
            for (OutputStream stream : streams) {
                try {
                    stream.write(bytes);
                    stream.flush();
                } catch (java.io.IOException e) {
                    // Silently ignore write errors to avoid infinite loops
                    System.err.println("Logger: Failed to write to stream: " + e.getMessage());
                }
            }
        }
    }

    private static String formatMessage(LogLevel level, String message) {
        // Prepare values for placeholders
        String levelStr = level.getDisplayName();
        String result = formatString;

        // Replace %TIMESTAMP with timestamp (if enabled)
        // Note: timestampFormat is validated when set, so we can safely use it here
        String timestamp = "";
        if (showTimestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat(timestampFormat);
            timestamp = sdf.format(new Date());
        }

        // Replace %MODULE with module name (if enabled, truncated/padded to max length)
        String moduleName = "";
        if (showModuleName && currentModuleName != null) {
            moduleName = currentModuleName;
            // Only truncate if maxModuleNameLength is not -1 (no truncation)
            if (maxModuleNameLength != -1 && moduleName.length() > maxModuleNameLength) {
                moduleName = moduleName.substring(0, maxModuleNameLength);
            } else if (forceModuleWidth && maxModuleNameLength != -1
                    && moduleName.length() < maxModuleNameLength) {
                // Force module width to max for consistent spacing (only if max length is set)
                moduleName = String.format("%-" + maxModuleNameLength + "s", moduleName);
            }
        } else if (forceModuleWidth && showModuleName && maxModuleNameLength != -1) {
            // Even if no module name, pad to max width for consistent spacing (only if max length
            // is set)
            moduleName = String.format("%-" + maxModuleNameLength + "s", "");
        }

        // Process placeholders with width specifiers using regex
        // Pattern matches: %[optional minus][optional width][optional
        // .precision](LEVEL|TIMESTAMP|MODULE|MESSAGE)

        // Process placeholders (only full names, no single letters)
        result = processPlaceholder(result,
                "%(-?)(\\d+)?(?:\\.(\\d+))?(LEVEL|TIMESTAMP|MODULE|MESSAGE)", levelStr, timestamp,
                moduleName, message);

        return result;
    }

    private static String processPlaceholder(String format, String patternRegex, String levelStr,
            String timestamp, String moduleName, String message) {
        Pattern pattern = Pattern.compile(patternRegex);
        Matcher matcher = pattern.matcher(format);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String leftAlign = matcher.group(1); // "-" for left align, null for right
            String widthStr = matcher.group(2); // Width number
            String precisionStr = matcher.group(3); // Precision number (for truncation)
            String placeholder = matcher.group(4); // The actual placeholder letter/word

            boolean isLeftAlign = "-".equals(leftAlign);
            int width = widthStr != null ? Integer.parseInt(widthStr) : -1;
            int precision = precisionStr != null ? Integer.parseInt(precisionStr) : -1;

            String value = "";
            switch (placeholder) {
                case "LEVEL":
                    value = levelStr;
                    break;
                case "TIMESTAMP":
                    value = timestamp;
                    break;
                case "MODULE":
                    value = moduleName;
                    break;
                case "MESSAGE":
                    value = message;
                    break;
                default:
                    value = "";
            }

            // Apply precision (truncation) if specified
            if (precision > 0 && value.length() > precision) {
                value = value.substring(0, precision);
            }

            // Apply width (padding/truncation) if specified
            if (width > 0) {
                if (value.length() > width) {
                    value = value.substring(0, width);
                } else {
                    // Pad to width
                    if (isLeftAlign) {
                        value = String.format("%-" + width + "s", value);
                    } else {
                        value = String.format("%" + width + "s", value);
                    }
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static void reset() {
        enabled = true;
        showTimestamp = false;
        showModuleName = false;
        currentModuleName = null;
        minLogLevel = LogLevel.DEBUG;
        formatString = DEFAULT_FORMAT_STRING;
        maxModuleNameLength = DEFAULT_MAX_MODULE_NAME_LENGTH;
        forceModuleWidth = DEFAULT_FORCE_MODULE_WIDTH;
        timestampFormat = DEFAULT_TIMESTAMP_FORMAT;

        // Reset streams to defaults (clearAllStreams already ensures System.out/err remain)
        clearAllStreams();
    }

    static class MultiOutputStream extends OutputStream {
        List<OutputStream> streams;

        public MultiOutputStream(OutputStream... streams) {
            this.streams = new ArrayList<>();
            for (OutputStream stream : streams) {
                if (stream != null) {
                    this.streams.add(stream);
                }
            }
        }

        @Override
        public void write(int b) throws java.io.IOException {
            for (OutputStream stream : streams) {
                stream.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            for (OutputStream stream : streams) {
                stream.write(b, off, len);
            }
        }

        @Override
        public void flush() throws java.io.IOException {
            for (OutputStream stream : streams) {
                stream.flush();
            }
        }

        @Override
        public void close() throws java.io.IOException {
            for (OutputStream stream : streams) {
                stream.close();
            }
        }
    }
}
