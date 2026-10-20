package redactedrice.randomizer.scripttests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Shared run loop for CLI and programmatic hosts (e.g. JUnit).
public final class ScriptTestBatchRunner {
    private ScriptTestBatchRunner() {}

    public static ScriptTestRunResult runRequestedCases(Path testsDir, String requestedName,
            ScriptTestWrapperFactory wrapperFactory, ScriptTestFixtures fixtures)
            throws IOException {
        return runCaseFiles(ScriptTestCli.selectCases(testsDir, requestedName), wrapperFactory,
                fixtures);
    }

    public static ScriptTestRunResult runCaseFiles(List<Path> caseFiles,
            ScriptTestWrapperFactory wrapperFactory, ScriptTestFixtures fixtures) {
        if (wrapperFactory == null) {
            throw new IllegalArgumentException("Wrapper factory cannot be null");
        }
        if (fixtures == null) {
            throw new IllegalArgumentException("Fixtures cannot be null");
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
                ScriptTestSession session =
                        new ScriptTestSession(wrapperFactory.create(), fixtures);
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
