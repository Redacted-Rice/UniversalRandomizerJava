package redactedrice.randomizer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import redactedrice.randomizer.lua.requirements.CoreRequirements;
import redactedrice.randomizer.utils.VersionProperties;

/**
 * Version identifiers for Universal Randomizer Java and the bundled Lua core library.
 */
public final class UniversalRandomizerVersions {
    public static final String JAVA_KEY = "UniversalRandomizerJava";
    public static final String CORE_KEY = "UniversalRandomizerCore";

    private static final Pattern CORE_VERSION_PATTERN =
            Pattern.compile("randomizer\\._VERSION\\s*=\\s*\"([^\"]+)\"");

    public static final String JAVA_VERSION = loadJavaVersion();
    public static final String CORE_VERSION = loadCoreVersion();

    private UniversalRandomizerVersions() {}

    /**
     * Registers optional library platform requirements on the given context.
     */
    public static void addTo(CoreRequirements requirements) {
        requirements.addCoreRequirement(JAVA_KEY, JAVA_VERSION, false);
        requirements.addCoreRequirement(CORE_KEY, CORE_VERSION, false);
    }

    private static String loadJavaVersion() {
        return VersionProperties.loadVersion(UniversalRandomizerVersions.class,
                "/redactedrice/randomizer/urj-version.properties");
    }

    private static String loadCoreVersion() {
        try (InputStream input =
                UniversalRandomizerVersions.class.getResourceAsStream("/randomizer/init.lua")) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing randomizer/init.lua on classpath; run a Gradle build first.");
            }
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = CORE_VERSION_PATTERN.matcher(content);
            if (!matcher.find()) {
                throw new IllegalStateException(
                        "Could not parse randomizer._VERSION from init.lua");
            }
            String version = matcher.group(1);
            if (version.isBlank()) {
                throw new IllegalStateException("randomizer._VERSION is empty in init.lua");
            }
            return version;
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

}
