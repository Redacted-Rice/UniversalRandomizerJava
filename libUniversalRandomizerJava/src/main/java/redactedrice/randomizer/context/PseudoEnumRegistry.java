package redactedrice.randomizer.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// manages multiple pseudo enum registries like modulegroup and modulemodifies
// these are string based enums that modules can extend
public class PseudoEnumRegistry {
    Map<String, Set<String>> registries;

    public PseudoEnumRegistry() {
        this.registries = new ConcurrentHashMap<>();
    }

    public void registerEnum(String registryName, String... values) {
        if (registryName == null || registryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Registry name cannot be null or empty");
        }

        // create registry if it doesnt exist yet
        registries.computeIfAbsent(registryName, k -> ConcurrentHashMap.newKeySet());

        // add all provided values to the registry
        Set<String> registry = registries.get(registryName);
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                // Make lowercase for case-insensitive matching
                registry.add(value.toLowerCase().trim());
            }
        }
    }

    public void extendEnum(String registryName, String... values) {
        registerEnum(registryName, values); // Same behavior
    }

    public boolean hasValue(String registryName, String value) {
        if (registryName == null || value == null) {
            return false;
        }

        Set<String> registry = registries.get(registryName);
        return registry != null && registry.contains(value.toLowerCase().trim());
    }

    public Set<String> getValues(String registryName) {
        Set<String> registry = registries.get(registryName);
        return registry != null ? Collections.unmodifiableSet(new LinkedHashSet<>(registry))
                : Collections.emptySet();
    }

    public Set<String> getRegistryNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(registries.keySet()));
    }

    public boolean hasRegistry(String registryName) {
        return registries.containsKey(registryName);
    }

    public void clearRegistry(String registryName) {
        Set<String> registry = registries.get(registryName);
        if (registry != null) {
            registry.clear();
        }
    }

    public void clearAll() {
        registries.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PseudoEnumRegistry{\n");
        for (Map.Entry<String, Set<String>> entry : registries.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue())
                    .append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
