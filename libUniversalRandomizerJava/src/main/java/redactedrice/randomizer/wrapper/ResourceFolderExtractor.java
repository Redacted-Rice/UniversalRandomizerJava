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

public class ResourceFolderExtractor {

    public static void extract(String resourcePath, String outputPath, boolean overwriteExisting)
            throws IOException {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }

        Path targetDir = Paths.get(outputPath);

        if (overwriteExisting && Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Read manifest to get list of files
        List<String> files = readManifest(resourcePath);

        // Copy each file
        for (String file : files) {
            Path targetFile = targetDir.resolve(file);

            if (Files.exists(targetFile) && !overwriteExisting) {
                continue;
            }

            Files.createDirectories(targetFile.getParent());

            String fullResourcePath = resourcePath + "/" + file;
            try (InputStream in = ResourceFolderExtractor.class.getClassLoader()
                    .getResourceAsStream(fullResourcePath)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + fullResourcePath);
                }
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static List<String> readManifest(String resourcePath) throws IOException {
        String manifestFile = resourcePath + "/.manifest";
        try (InputStream manifestStream =
                ResourceFolderExtractor.class.getClassLoader().getResourceAsStream(manifestFile)) {
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

