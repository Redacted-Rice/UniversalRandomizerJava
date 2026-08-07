package redactedrice.randomizer.lua;

public final class Issue {
    private final Module module;
    private final String subject;
    private final String category;
    private final boolean isError;
    private final String message;

    public Issue(Module module, String subject, String category, boolean isError, String message) {
        this.module = module;
        this.subject = subject;
        this.category = category;
        this.isError = isError;
        this.message = message;
    }

    public Module getModule() {
        return module;
    }

    public String getSubject() {
        return subject;
    }

    public String getCategory() {
        return category;
    }

    public boolean isError() {
        return isError;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
