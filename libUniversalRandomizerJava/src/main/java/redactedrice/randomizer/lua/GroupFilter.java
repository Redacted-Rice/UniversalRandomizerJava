package redactedrice.randomizer.lua;

import redactedrice.randomizer.utils.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Filters modules based on their group metadata
public class GroupFilter implements ModuleFilter {
    private final Set<String> allowedGroups;

    public GroupFilter(Set<String> allowedGroups) {
        this.allowedGroups = normalizeStringSet(allowedGroups);
    }

    @Override
    public boolean accepts(Module module) {
        if (module == null) {
            return false;
        }

        // If no filter is defined, accept all
        if (allowedGroups == null || allowedGroups.isEmpty()) {
            return true;
        }

        Set<String> moduleGroups = module.getGroups();
        if (moduleGroups == null || moduleGroups.isEmpty()) {
            return false;
        }

        boolean hasMatch = false;
        for (String group : moduleGroups) {
            if (group != null && !group.trim().isEmpty()) {
                if (allowedGroups.contains(group.toLowerCase())) {
                    hasMatch = true;
                } else {
                    Logger.warn("Module '" + module.getName() + "' has group '" + group
                            + "' which is not in defined groups values");
                }
            }
        }

        if (!hasMatch) {
            Logger.warn("Ignoring module '" + module.getName()
                    + "' - no groups values in defined list");
        }

        return hasMatch;
    }

    private Set<String> normalizeStringSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized.isEmpty() ? null : Collections.unmodifiableSet(normalized);
    }
}
