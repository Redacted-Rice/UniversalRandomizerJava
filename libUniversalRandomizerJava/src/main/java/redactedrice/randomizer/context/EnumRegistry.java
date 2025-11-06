package redactedrice.randomizer.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// registry for dynamic enum values that modules can extend
public class EnumRegistry {
    String registryName;
    Set<String> coreValues;
    Set<String> customValues;
    Map<String, String> valueDescriptions;

    public EnumRegistry(String registryName, String... coreValues) {
        this.registryName = registryName;
        this.coreValues =
                Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(coreValues)));
        this.customValues = ConcurrentHashMap.newKeySet();
        this.valueDescriptions = new ConcurrentHashMap<>();

        // Initialize core value descriptions
        for (String value : coreValues) {
            this.valueDescriptions.put(value, "Core " + registryName + " value");
        }
    }

    public void registerCustomValue(String value, String description) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value cannot be null or empty");
        }

        // make lowercase for case-insensitive matching
        String normalizedValue = value.trim().toLowerCase();

        // don't reregister core values
        if (coreValues.contains(normalizedValue)) {
            // already a core value, no need to register
            return;
        }

        // add to custom values set
        customValues.add(normalizedValue);
        // store description (use default if none provided)
        if (description != null && !description.isEmpty()) {
            valueDescriptions.put(normalizedValue, description);
        } else {
            valueDescriptions.put(normalizedValue, "Custom " + registryName + " value");
        }
    }

    public void registerCustomValue(String value) {
        registerCustomValue(value, null);
    }

    public boolean isRegistered(String value) {
        if (value == null) {
            return false;
        }
        String normalizedValue = value.trim().toLowerCase();
        return coreValues.contains(normalizedValue) || customValues.contains(normalizedValue);
    }

    public Set<String> getAllValues() {
        Set<String> all = new LinkedHashSet<>(coreValues);
        all.addAll(customValues);
        return Collections.unmodifiableSet(all);
    }

    public Set<String> getCoreValues() {
        return coreValues;
    }

    public Set<String> getCustomValues() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(customValues));
    }

    public String getDescription(String value) {
        if (value == null) {
            return null;
        }
        return valueDescriptions.get(value.trim().toLowerCase());
    }

    public String getRegistryName() {
        return registryName;
    }

    public void clearCustomValues() {
        customValues.clear();
    }

    @Override
    public String toString() {
        return registryName + " [" + getAllValues().size() + " values: " + coreValues.size()
                + " core, " + customValues.size() + " custom]";
    }

    // Predefined registries
    private static final EnumRegistry MODULE_GROUPS = new EnumRegistry("ModuleGroup", "gameplay", // Affects
                                                                                                  // game
                                                                                                  // mechanics
                                                                                                  // and
                                                                                                  // rules
            "visual", // Changes visual appearance
            "audio", // Modifies sound/music
            "balance", // Adjusts difficulty and balance
            "content", // Adds or modifies content
            "utility", // Helper/utility functions
            "experimental" // Experimental features
    );

    private static final EnumRegistry MODULE_MODIFIES = new EnumRegistry("ModuleModifies", "stats", // Character/entity
                                                                                                    // statistics
            "appearance", // Visual properties
            "behavior", // AI or behavior patterns
            "loot", // Item drops and rewards
            "difficulty", // Challenge level
            "progression", // Level/experience systems
            "economy", // Currency and trading
            "environment" // World/level properties
    );

    // Get the global module groups registry
    public static EnumRegistry getModuleGroups() {
        return MODULE_GROUPS;
    }

    // Get the global module modifies registry
    public static EnumRegistry getModuleModifies() {
        return MODULE_MODIFIES;
    }
}
