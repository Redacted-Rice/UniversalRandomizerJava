package redactedrice.randomizer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ManifestResourceExtractor {

    // Always just overlays the manifest's files onto outputPath, overwriting whichever of them
    // are already there. Never touches anything else under outputPath (existing files not in the
    // manifest are left alone), so it's safe to call on a directory shared with other content.
    // Callers that want stale/edited files preserved instead of silently overwritten should back
    // them up first (see VersionedResourceInstaller).
    public static void extract(String resourcePath, String outputPath) throws IOException {
        extract(resourcePath, outputPath, ManifestResourceExtractor.class.getClassLoader());
    }

    public static void extract(String resourcePath, String outputPath, ClassLoader classLoader)
            throws IOException {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
        ClassLoader loader = Objects.requireNonNull(classLoader, "ClassLoader cannot be null");

        Path targetDir = Paths.get(outputPath);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        List<String> files = readManifest(resourcePath, loader);

        for (String file : files) {
            Path targetFile = targetDir.resolve(file);
            Files.createDirectories(targetFile.getParent());

            String fullResourcePath = resourcePath + "/" + file;
            try (InputStream in = loader.getResourceAsStream(fullResourcePath)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + fullResourcePath);
                }
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // JAR entries dont carry the unix exec bit so shell wrapper scripts extract as
            // non executable. Fix that up here instead of making every caller remember to chmod.
            if (file.endsWith(".sh")) {
                targetFile.toFile().setExecutable(true, false);
            }
        }
    }

    // Exposed so hosts can enumerate what a resource ships (e.g. to back up specific files
    // before an overwrite) without duplicating manifest parsing.
    public static List<String> listFiles(String resourcePath) throws IOException {
        return listFiles(resourcePath, ManifestResourceExtractor.class.getClassLoader());
    }

    public static List<String> listFiles(String resourcePath, ClassLoader classLoader)
            throws IOException {
        return readManifest(resourcePath, Objects.requireNonNull(classLoader));
    }

    private static List<String> readManifest(String resourcePath, ClassLoader classLoader)
            throws IOException {
        String manifestFile = resourcePath + "/.manifest";
        try (InputStream manifestStream = classLoader.getResourceAsStream(manifestFile)) {
            if (manifestStream == null) {
                throw new IOException("Manifest file not found: " + manifestFile);
            }

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(manifestStream))) {
                return reader.lines().map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toList());
            }
        }
    }
}
