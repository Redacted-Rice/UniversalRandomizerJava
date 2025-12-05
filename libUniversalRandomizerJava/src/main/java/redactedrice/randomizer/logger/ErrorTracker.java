package redactedrice.randomizer.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Not super happy with this ATM but leaving it for now to
// and focusing on getting things working again
public class ErrorTracker {
    private static final List<String> errors = new ArrayList<>();

    public static void addError(String error) {
        errors.add(error);
        Logger.error(error);
    }

    public static List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public static boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static int getErrorCount() {
        return errors.size();
    }

    public static void clearErrors() {
        errors.clear();
    }
}
