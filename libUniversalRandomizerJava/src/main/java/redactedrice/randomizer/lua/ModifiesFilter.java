package redactedrice.randomizer.lua;

import redactedrice.randomizer.utils.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Filters modules based on their modifies metadata
public class ModifiesFilter implements ModuleFilter {
    private final Set<String> allowedModifies;

    public ModifiesFilter(Set<String> allowedModifies) {
        this.allowedModifies = normalizeStringSet(allowedModifies);
    }

    @Override
    public boolean accepts(Module module) {
        if (module == null) {
            return false;
        }

        // If no filter is defined, accept all
        if (allowedModifies == null || allowedModifies.isEmpty()) {
            return true;
        }

        Set<String> moduleModifies = module.getModifies();
        if (moduleModifies == null || moduleModifies.isEmpty()) {
            return false;
        }

        boolean hasMatch = false;
        for (String modifies : moduleModifies) {
            if (modifies != null && !modifies.trim().isEmpty()) {
                if (allowedModifies.contains(modifies.toLowerCase())) {
                    hasMatch = true;
                } else {
                    Logger.warn("Module '" + module.getName() + "' has modifies '" + modifies
                            + "' which is not in defined modifies values");
                }
            }
        }

        if (!hasMatch) {
            Logger.warn("Ignoring module '" + module.getName()
                    + "' - no modifies values in defined list");
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
