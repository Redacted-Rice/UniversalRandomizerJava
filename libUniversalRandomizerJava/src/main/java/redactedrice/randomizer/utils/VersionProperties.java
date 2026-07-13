package redactedrice.randomizer.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads a version property from a classpath resource generated at build time
 */
public final class VersionProperties {
    private VersionProperties() {}

    public static String loadVersion(Class<?> anchor, String resourcePath) {
        try (InputStream input = anchor.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing " + resourcePath + "; run a Gradle build first.");
            }
            Properties properties = new Properties();
            properties.load(input);
            String version = properties.getProperty("version");
            if (version == null || version.isBlank()) {
                throw new IllegalStateException(resourcePath + " is missing version.");
            }
            return version;
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
