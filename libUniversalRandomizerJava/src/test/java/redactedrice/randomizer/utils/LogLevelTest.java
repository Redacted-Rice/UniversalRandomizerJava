package redactedrice.randomizer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LogLevelTest {

    @Test
    void parseAcceptsCaseInsensitiveNames() {
        assertEquals(LogLevel.DEBUG, LogLevel.parse("debug"));
        assertEquals(LogLevel.INFO, LogLevel.parse("INFO"));
        assertEquals(LogLevel.WARN, LogLevel.parse(" warn "));
        assertEquals(LogLevel.ERROR, LogLevel.parse("Error"));
    }

    @Test
    void parseRejectsBlankAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> LogLevel.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.parse("trace"));
    }
}
