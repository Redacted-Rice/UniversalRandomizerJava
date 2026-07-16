package redactedrice.randomizer.lua.requirements;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * App provided platform versions and validation policy for module requires metadata. Version
 * strings in requires are treated as minimum compatible versions.
 */
public final class CoreRequirements {
    private final Map<String, CoreRequirement> requirements;

    public CoreRequirements() {
        this.requirements = new HashMap<>();
    }

    public void addCoreRequirement(String key, String currentVersion, boolean isMandatory) {
        requirements.put(key, new CoreRequirement(key, currentVersion, isMandatory));
    }

    public Collection<CoreRequirement> getRequirements() {
        return requirements.values();
    }

    public CoreRequirement getRequirement(String key) {
        return requirements.get(key);
    }
}
