package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.utils.Logger;

// Headless runner for Lua case files in a folder. Hosts wire this to a --script-tests flag.
public final class ScriptTestCli {
    public static final String FLAG = "--script-tests";
    // Case files use a test_ prefix so helpers at the folder root are not picked up by mistake.
    private static final Pattern CASE_FILE_NAME = Pattern.compile("test_.+\\.lua",
            Pattern.CASE_INSENSITIVE);

    private ScriptTestCli() {}

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && FLAG.equals(args[0]);
    }

    public static int run(String[] args, Path testsDir, LuaRandomizerWrapper wrapper,
            ScriptTestFixtures fixtures) {
        if (!handles(args)) {
            throw new IllegalArgumentException("Missing " + FLAG);
        }
        if (args.length > 2) {
            System.err.println("Usage: " + FLAG + " [test-file]");
            return 2;
        }
        if (wrapper == null) {
            throw new IllegalArgumentException("Wrapper cannot be null");
        }
        if (fixtures == null) {
            throw new IllegalArgumentException("Fixtures cannot be null");
        }

        List<Path> cases;
        try {
            cases = selectCases(testsDir, args.length == 2 ? args[1] : null);
        } catch (IOException e) {
            System.err.println("Failed to list script tests: " + e.getMessage());
            return 2;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        if (cases.isEmpty()) {
            System.err.println("No lua tests found in " + testsDir.toAbsolutePath());
            return 2;
        }

        Logger.setEnabled(true);
        ScriptTestSession session = new ScriptTestSession(wrapper, fixtures);

        int passed = 0;
        int failed = 0;
        for (Path caseFile : cases) {
            List<ScriptTestCase> fileCases;
            try {
                fileCases = ScriptTestCase.loadAll(caseFile);
            } catch (Exception e) {
                System.out.println(caseFile.getFileName() + " FAIL");
                System.out.println("  " + e.getMessage());
                failed++;
                continue;
            }

            for (ScriptTestCase testCase : fileCases) {
                String name = testCase.displayName();
                System.out.println("Running " + name);
                try {
                    session.run(testCase);
                    System.out.println(name + " PASS");
                    passed++;
                } catch (Exception e) {
                    System.out.println(name + " FAIL");
                    System.out.println("  " + e.getMessage());
                    failed++;
                }
            }
        }

        System.out.println(passed + " passed, " + failed + " failed");
        return failed == 0 ? 0 : 1;
    }

    static List<Path> selectCases(Path testsDir, String requestedName) throws IOException {
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
            return files.filter(ScriptTestCli::isLuaCase).sorted().toList();
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
            files.filter(ScriptTestCli::isLuaCase)
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
