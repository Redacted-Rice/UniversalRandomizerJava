package redactedrice.randomizer.wrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class RandomizerResourceExtractor {
    private static final String RANDOMIZER_RESOURCE_PATH = "randomizer";
    // TODO: Look into this more. It seems like there should be a way to just copy
    // the whole directory but it doesn't seem to be for a JAR
    private static final String[] RANDOMIZER_FILES =
            {"init.lua", "list.lua", "group.lua", "utils.lua", "changedetector.lua"};
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
        File targetDir = new File(extractionPath);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + extractionPath);
            }
        }

        if (!targetDir.isDirectory()) {
            throw new IOException("Target path is not a directory: " + extractionPath);
        }

        // Extract all randomizer files
        for (String fileName : RANDOMIZER_FILES) {
            File targetFile = new File(targetDir, fileName);

            // Skip if file exists and we're not overwriting
            if (targetFile.exists() && !overwriteExisting) {
                continue;
            }

            String resourcePath = RANDOMIZER_RESOURCE_PATH + "/" + fileName;
            InputStream resourceStream = RandomizerResourceExtractor.class.getClassLoader()
                    .getResourceAsStream(resourcePath);

            if (resourceStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            try {
                Path targetPath = targetFile.toPath();
                if (overwriteExisting) {
                    Files.copy(resourceStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.copy(resourceStream, targetPath);
                }
            } finally {
                resourceStream.close();
            }
        }
    }
}

