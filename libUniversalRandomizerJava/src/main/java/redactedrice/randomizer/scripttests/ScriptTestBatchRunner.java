package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Shared run loop for CLI and programmatic hosts (e.g. JUnit).
public final class ScriptTestBatchRunner {
    private ScriptTestBatchRunner() {}

    public static ScriptTestRunResult runRequestedCases(Path testsDir, String requestedName,
            ScriptTestSession session) throws IOException {
        return runCaseFiles(ScriptTestCli.selectCases(testsDir, requestedName), session);
    }

    public static ScriptTestRunResult runCaseFiles(List<Path> caseFiles,
            ScriptTestSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        if (caseFiles == null) {
            throw new IllegalArgumentException("Case files cannot be null");
        }

        int passed = 0;
        int failed = 0;
        List<ScriptTestFailure> failures = new ArrayList<>();

        for (Path caseFile : caseFiles) {
            List<ScriptTestCase> fileCases;
            try {
                fileCases = ScriptTestCase.loadAll(caseFile);
            } catch (Exception e) {
                failed++;
                failures.add(new ScriptTestFailure(caseFile.getFileName().toString(),
                        e.getMessage()));
                continue;
            }

            for (ScriptTestCase testCase : fileCases) {
                try {
                    session.run(testCase);
                    passed++;
                } catch (Exception e) {
                    failed++;
                    failures.add(new ScriptTestFailure(testCase.displayName(), e.getMessage()));
                }
            }
        }

        return new ScriptTestRunResult(passed, failed, List.copyOf(failures));
    }
}
