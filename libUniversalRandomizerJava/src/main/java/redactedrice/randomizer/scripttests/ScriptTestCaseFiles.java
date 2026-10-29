package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Discover lua case files under a script_tests folder. Host CLIs pick files then call the batch runner.
public final class ScriptTestCaseFiles {
    // Case files use a test_ prefix so helpers at the folder root are not picked up by mistake.
    private static final Pattern CASE_FILE_NAME = Pattern.compile("test_.+\\.lua",
            Pattern.CASE_INSENSITIVE);

    private ScriptTestCaseFiles() {}

    public static List<Path> selectCases(Path testsDir, String requestedName) throws IOException {
        if (testsDir == null || !Files.isDirectory(testsDir)) {
            throw new IllegalArgumentException("script_tests folder is missing"
                    + (testsDir == null ? "" : ": " + testsDir.toAbsolutePath()));
        }

        if (requestedName == null || requestedName.isBlank()) {
            return listAllCases(testsDir);
        }

        return List.of(findCaseFile(testsDir, requestedName));
    }

    private static List<Path> listAllCases(Path testsDir) throws IOException {
        try (Stream<Path> files = Files.walk(testsDir)) {
            return files.filter(ScriptTestCaseFiles::isLuaCase).sorted().toList();
        }
    }

    private static Path findCaseFile(Path testsDir, String requestedName) throws IOException {
        String fileName = requestedName;
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".lua")) {
            fileName = fileName + ".lua";
        }
        final String matchName = fileName;
        if (matchName.contains("/") || matchName.contains("\\") || matchName.contains("..")) {
            throw new IllegalArgumentException("Pass a test file name, not a path");
        }

        Path direct = testsDir.resolve(matchName);
        if (Files.isRegularFile(direct)) {
            if (!isLuaCase(direct)) {
                throw new IllegalArgumentException(
                        "Script test files must match test_name.lua, got '" + matchName + "'");
            }
            return direct;
        }

        List<Path> matches = new ArrayList<>();
        try (Stream<Path> files = Files.walk(testsDir)) {
            files.filter(ScriptTestCaseFiles::isLuaCase)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(matchName))
                    .forEach(matches::add);
        }
        matches.sort(Path::compareTo);

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No script test named '" + matchName + "' under "
                    + testsDir.toAbsolutePath());
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Multiple script tests named '" + matchName + "': "
                    + matches.stream().map(path -> testsDir.relativize(path).toString()).toList());
        }
        return matches.get(0);
    }

    static boolean isLuaCase(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        return isLuaCaseFileName(path.getFileName().toString());
    }

    public static boolean isLuaCaseFileName(String fileName) {
        return CASE_FILE_NAME.matcher(fileName).matches();
    }
}
