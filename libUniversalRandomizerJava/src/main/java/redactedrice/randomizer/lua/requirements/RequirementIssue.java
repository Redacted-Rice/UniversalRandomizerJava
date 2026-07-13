package redactedrice.randomizer.lua.requirements;

import redactedrice.randomizer.lua.Module;

public final class RequirementIssue {
    private final Module module;
    private final String requirementKey;
    private final boolean isError;
    private final String message;

    public RequirementIssue(Module module, String requirementKey, boolean isError, String message) {
        this.module = module;
        this.requirementKey = requirementKey;
        this.isError = isError;
        this.message = message;
    }

    public Module getModule() {
        return module;
    }

    public String getRequirementKey() {
        return requirementKey;
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
