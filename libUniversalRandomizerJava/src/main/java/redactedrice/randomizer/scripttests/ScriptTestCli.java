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
import redactedrice.randomizer.utils.LogLevel;
import redactedrice.randomizer.utils.Logger;

// Headless runner for Lua case files in a folder. Hosts wire this to a --script-tests flag.
public final class ScriptTestCli {
    public static final String FLAG = "--script-tests";
    public static final String LOG_LEVEL_FLAG = "--log-level";
    public static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.WARN;
    // Case files use a test_ prefix so helpers at the folder root are not picked up by mistake.
    private static final Pattern CASE_FILE_NAME = Pattern.compile("test_.+\\.lua",
            Pattern.CASE_INSENSITIVE);

    private ScriptTestCli() {}

    public static final class RunOptions {
        private final LogLevel logLevel;
        private final String testFile;

        RunOptions(LogLevel logLevel, String testFile) {
            this.logLevel = logLevel;
            this.testFile = testFile;
        }

        public LogLevel logLevel() {
            return logLevel;
        }

        public String testFile() {
            return testFile;
        }
    }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && FLAG.equals(args[0]);
    }

    static RunOptions parseRunOptions(String[] args) {
        if (!handles(args)) {
            throw new IllegalArgumentException("Missing " + FLAG);
        }
        LogLevel logLevel = DEFAULT_LOG_LEVEL;
        String testFile = null;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (LOG_LEVEL_FLAG.equals(arg)) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException(LOG_LEVEL_FLAG + " requires a level");
                }
                logLevel = LogLevel.parse(args[++i]);
                continue;
            }
            if (arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown option '" + arg + "'");
            }
            if (testFile != null) {
                throw new IllegalArgumentException("Pass at most one test file name");
            }
            testFile = arg;
        }
        return new RunOptions(logLevel, testFile);
    }

    public static int run(String[] args, Path testsDir, LuaRandomizerWrapper wrapper,
            ScriptTestFixtures fixtures) {
        RunOptions options;
        try {
            options = parseRunOptions(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(usage());
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
            cases = selectCases(testsDir, options.testFile());
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
        Logger.setMinLogLevel(options.logLevel());
        ScriptTestSession session = new ScriptTestSession(wrapper, fixtures);
        ScriptTestRunResult result = ScriptTestBatchRunner.runCaseFiles(cases, session);
        printResult(result);
        return result.exitCode();
    }

    private static void printResult(ScriptTestRunResult result) {
        for (ScriptTestFailure failure : result.failures()) {
            System.out.println(failure.displayName() + " FAIL");
            if (failure.message() != null && !failure.message().isBlank()) {
                System.out.println("  " + failure.message());
            }
        }
        System.out.println(result.passed() + " passed, " + result.failed() + " failed");
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

    private static String usage() {
        return "Usage: " + FLAG + " [" + LOG_LEVEL_FLAG + " LEVEL] [test-file]"
                + System.lineSeparator()
                + "  LEVEL is DEBUG, INFO, WARN, or ERROR (default "
                + DEFAULT_LOG_LEVEL.name() + ")";
    }
}
