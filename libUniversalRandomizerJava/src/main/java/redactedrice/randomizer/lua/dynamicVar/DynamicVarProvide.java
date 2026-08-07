package redactedrice.randomizer.lua.dynamicVar;

import redactedrice.randomizer.lua.Module;

/** One declared provide on a module */
public final class DynamicVarProvide {
    private final Module module;
    private final DynamicVar definition;

    public DynamicVarProvide(Module module, DynamicVar definition) {
        this.module = module;
        this.definition = definition;
    }

    public Module getModule() {
        return module;
    }

    public DynamicVar getDefinition() {
        return definition;
    }

    public String getModuleId() {
        return module.getId();
    }
}
