package redactedrice.randomizer;

import redactedrice.randomizer.utils.VersionProperties;

/**
 * Example application version recorded in module requires metadata. Loaded from
 * app-version.properties, which is generated from build.gradle.kts at build time.
 */
public final class ExampleAppVersion {
    public static final String PLATFORM_KEY = "ExampleApp";
    public static final String VERSION = loadVersion();

    private ExampleAppVersion() {}

    private static String loadVersion() {
        return VersionProperties.loadVersion(ExampleAppVersion.class, "app-version.properties");
    }
}
