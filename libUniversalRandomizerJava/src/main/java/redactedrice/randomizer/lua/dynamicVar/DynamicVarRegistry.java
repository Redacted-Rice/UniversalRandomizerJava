package redactedrice.randomizer.lua.dynamicVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRepository;

/**
 * Load time registry of declared providers and consumer needs. metadata only. built in two passes
 * so provider collection finishes before any need is checked.
 */
public final class DynamicVarRegistry {
    private List<DynamicVarProvide> allProviders = List.of();
    private Map<String, List<DynamicVarProvide>> providersByName = Map.of();
    private Map<String, List<DynamicVarNeed>> needsByConsumerId = Map.of();

    public static DynamicVarRegistry empty() {
        return new DynamicVarRegistry();
    }

    public void buildFrom(ModuleRepository repository) {
        List<Module> allModules = repository.getAllModulesAndScripts();
        List<DynamicVarProvide> nextProviders = new ArrayList<>();
        Map<String, List<DynamicVarProvide>> nextProvidersByName = new LinkedHashMap<>();

        // pass 1 - collect every provide from every loaded module/script
        for (Module module : allModules) {
            for (DynamicVar provide : module.getProvides()) {
                DynamicVarProvide provider = new DynamicVarProvide(module, provide);
                nextProviders.add(provider);
                nextProvidersByName.computeIfAbsent(provide.getName(), ignored -> new ArrayList<>())
                        .add(provider);
            }
        }

        // pass 2 - Validate needs against the full provider list
        Map<String, List<DynamicVarNeed>> nextNeedsByConsumerId = new LinkedHashMap<>();
        for (Module module : allModules) {
            if (module.getNeeds().isEmpty()) {
                continue;
            }

            List<DynamicVarNeed> bindings = new ArrayList<>();
            for (DynamicVar need : module.getNeeds()) {
                bindings.add(new DynamicVarNeed(module, need,
                        findCompatibleProviders(nextProviders, need)));
            }
            nextNeedsByConsumerId.put(module.getId(), List.copyOf(bindings));
        }

        allProviders = List.copyOf(nextProviders);
        providersByName = copyProviderMap(nextProvidersByName);
        needsByConsumerId = Map.copyOf(nextNeedsByConsumerId);
    }

    public void clear() {
        allProviders = List.of();
        providersByName = Map.of();
        needsByConsumerId = Map.of();
    }

    public List<DynamicVarProvide> getAllProviders() {
        return allProviders;
    }

    public List<DynamicVarProvide> getProvidersByName(String name) {
        return providersByName.getOrDefault(name, List.of());
    }

    public List<DynamicVarNeed> getNeedsForConsumer(String moduleId) {
        return needsByConsumerId.getOrDefault(moduleId, List.of());
    }

    public Map<String, List<DynamicVarNeed>> getNeedsByConsumerId() {
        return needsByConsumerId;
    }

    public List<DynamicVarProvide> findCompatibleProviders(DynamicVar need) {
        return findCompatibleProviders(allProviders, need);
    }

    public List<String> getCompatibleProviderModuleIds(DynamicVar need) {
        LinkedHashSet<String> moduleIds = new LinkedHashSet<>();
        for (DynamicVarProvide provider : findCompatibleProviders(need)) {
            moduleIds.add(provider.getModuleId());
        }
        return List.copyOf(moduleIds);
    }

    private static List<DynamicVarProvide> findCompatibleProviders(
            List<DynamicVarProvide> providers, DynamicVar need) {
        List<DynamicVarProvide> matches = new ArrayList<>();
        for (DynamicVarProvide provider : providers) {
            if (provider.getDefinition().satisfiesNeed(need)) {
                matches.add(provider);
            }
        }
        return List.copyOf(matches);
    }

    private static Map<String, List<DynamicVarProvide>> copyProviderMap(
            Map<String, List<DynamicVarProvide>> source) {
        Map<String, List<DynamicVarProvide>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<DynamicVarProvide>> entry : source.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
