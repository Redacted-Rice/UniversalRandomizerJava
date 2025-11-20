package redactedrice.randomizer.wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Extracts resources based on the manifest file which is assumed to be generated
// as part of the build process
public class ResourceFolderExtractor {
    private static final String RANDOMIZER_RESOURCE_PATH = "randomizer";
    private static final String MANIFEST_FILE = RANDOMIZER_RESOURCE_PATH + "/.manifest";
    private static String extractionPath = "randomizer";

    public static void setPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        extractionPath = path;
    }

    public static String getPath() {
        return extractionPath;
    }

    public static void extract(boolean overwriteExisting) throws IOException {
        Path targetDir = Paths.get(extractionPath);

        if (overwriteExisting && Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Read manifest to get list of files
        java.util.List<String> files = readManifest();

        // Copy each file
        for (String file : files) {
            Path targetFile = targetDir.resolve(file);

            if (Files.exists(targetFile) && !overwriteExisting) {
                continue;
            }

            Files.createDirectories(targetFile.getParent());

            String resourcePath = RANDOMIZER_RESOURCE_PATH + "/" + file;
            try (InputStream in = ResourceFolderExtractor.class.getClassLoader()
                    .getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + resourcePath);
                }
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static List<String> readManifest() throws IOException {
        try (InputStream manifestStream = ResourceFolderExtractor.class.getClassLoader()
                .getResourceAsStream(MANIFEST_FILE)) {
            if (manifestStream == null) {
                throw new IOException("Manifest file not found: " + MANIFEST_FILE);
            }

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(manifestStream))) {
                return reader.lines().map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toList());
            }
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory))
            return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}

