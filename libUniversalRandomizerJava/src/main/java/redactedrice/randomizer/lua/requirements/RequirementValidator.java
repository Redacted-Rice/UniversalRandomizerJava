package redactedrice.randomizer.lua.requirements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRepository;

/**
 * Validates module and script requires metadata against registered platform versions and loaded
 * module/script ids.
 */
public final class RequirementValidator {
    private RequirementValidator() {}

    public static List<RequirementIssue> validate(CoreRequirements coreReqs,
            ModuleRepository repository) {
        List<RequirementIssue> issues = new ArrayList<>();

        for (Module module : repository.getAllModulesAndScripts()) {
            validateModule(coreReqs, repository, module, issues);
        }
        return issues;
    }

    private static void validateModule(CoreRequirements coreReqs, ModuleRepository repository,
            Module module, List<RequirementIssue> issues) {
        Map<String, String> requires = module.getRequires();

        if (coreReqs != null) {
            for (CoreRequirement requirement : coreReqs.getRequirements()) {
                if (requirement.isMandatory() && !requires.containsKey(requirement.getKey())) {
                    issues.add(new RequirementIssue(module, requirement.getKey(), true,
                            moduleInfoString(module) + ": missing mandatory require '"
                                    + requirement.getKey() + "'"));
                }
            }
        }

        for (Map.Entry<String, String> entry : requires.entrySet()) {
            String key = entry.getKey();
            String requiredVersion = entry.getValue();
            CoreRequirement requirement = coreReqs != null ? coreReqs.getRequirement(key) : null;
            if (requirement != null) {
                if (!satisfiesMinimumVersion(requirement.getCurrentVersion(), requiredVersion)) {
                    issues.add(new RequirementIssue(module, key, false,
                            moduleInfoString(module) + ": requires minimum " + key + " "
                                    + requiredVersion + " but current version is "
                                    + requirement.getCurrentVersion()));
                }
                continue;
            }

            Module dependency = repository.getModule(key);
            if (dependency == null) {
                dependency = repository.getScript(key);
            }
            if (dependency == null) {
                issues.add(new RequirementIssue(module, key, true,
                        moduleInfoString(module) + ": requires '" + key + "' (" + requiredVersion
                                + ") but no loaded module or script with that id"));
                continue;
            }

            if (!satisfiesMinimumVersion(dependency.getVersion(), requiredVersion)) {
                issues.add(new RequirementIssue(module, key, false,
                        moduleInfoString(module) + ": requires minimum " + key + " "
                                + requiredVersion + " but loaded version is "
                                + dependency.getVersion()));
            }
        }
    }

    /**
     * Returns true when currentVersion is greater than or equal to requiredMinimum
     */
    static boolean satisfiesMinimumVersion(String currentVersion, String requiredMinimum) {
        try {
            return compareVersions(currentVersion, requiredMinimum) >= 0;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    static int compareVersions(String left, String right) {
        int[] leftParts = parseVersionParts(left);
        int[] rightParts = parseVersionParts(right);
        int maxLength = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < maxLength; i++) {
            int leftSegment = i < leftParts.length ? leftParts[i] : 0;
            int rightSegment = i < rightParts.length ? rightParts[i] : 0;
            if (leftSegment != rightSegment) {
                return Integer.compare(leftSegment, rightSegment);
            }
        }
        return 0;
    }

    static int[] parseVersionParts(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }

        String[] segments = version.trim().split("\\.");
        int[] parsed = new int[segments.length];
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("Invalid version: " + version);
            }
            try {
                parsed[i] = Integer.parseInt(segment);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException(
                        "Invalid version segment '" + segment + "' in version: " + version, error);
            }
        }
        return parsed;
    }

    private static String moduleInfoString(Module module) {
        String kind = module.isScript() ? "script" : "module";
        return module.getId() + " (" + module.getName() + ", " + kind + ")";
    }
}
