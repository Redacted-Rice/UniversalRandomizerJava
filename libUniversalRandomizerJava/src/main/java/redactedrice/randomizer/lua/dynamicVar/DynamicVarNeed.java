package redactedrice.randomizer.lua.dynamicVar;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import redactedrice.randomizer.lua.Module;

/** One declared need on a module plus compatible providers found at load time. */
public final class DynamicVarNeed {
    private final Module module;
    private final DynamicVar need;
    private final List<DynamicVarProvide> compatibleProviders;

    public DynamicVarNeed(Module module, DynamicVar need,
            List<DynamicVarProvide> compatibleProviders) {
        this.module = module;
        this.need = need;
        this.compatibleProviders = compatibleProviders;
    }

    public Module getModule() {
        return module;
    }

    public DynamicVar getNeed() {
        return need;
    }

    public List<DynamicVarProvide> getCompatibleProviders() {
        return compatibleProviders;
    }

    public boolean isSatisfied() {
        return !compatibleProviders.isEmpty();
    }

    public List<String> getCompatibleProviderModuleIds() {
        Set<String> moduleIds = new LinkedHashSet<>();
        for (DynamicVarProvide provider : compatibleProviders) {
            moduleIds.add(provider.getModuleId());
        }
        return List.copyOf(moduleIds);
    }
}
