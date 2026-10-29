package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import redactedrice.randomizer.utils.LogLevel;
import redactedrice.randomizer.utils.Logger;

// Headless runner for Lua case files in a folder. ExampleApp wires this to a --script-tests flag.
public final class ScriptTestCli {
    public static final String FLAG = "--script-tests";
    public static final String LOG_LEVEL_FLAG = "--log-level";
    public static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.WARN;

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

    public static int run(String[] args, Path testsDir, ScriptTestWrapperFactory wrapperFactory,
            ScriptTestFixtures fixtures) {
        RunOptions options;
        try {
            options = parseRunOptions(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(usage());
            return 2;
        }
        if (wrapperFactory == null) {
            throw new IllegalArgumentException("Wrapper factory cannot be null");
        }
        if (fixtures == null) {
            throw new IllegalArgumentException("Fixtures cannot be null");
        }

        List<Path> cases;
        try {
            cases = ScriptTestCaseFiles.selectCases(testsDir, options.testFile());
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
        ScriptTestRunResult result =
                ScriptTestBatchRunner.runCaseFiles(cases, wrapperFactory, fixtures);
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

    private static String usage() {
        return "Usage: " + FLAG + " [" + LOG_LEVEL_FLAG + " LEVEL] [test-file]"
                + System.lineSeparator()
                + "  LEVEL is DEBUG, INFO, WARN, or ERROR (default "
                + DEFAULT_LOG_LEVEL.name() + ")";
    }
}
