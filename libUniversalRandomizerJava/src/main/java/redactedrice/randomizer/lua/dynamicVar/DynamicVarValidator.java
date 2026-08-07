package redactedrice.randomizer.lua.dynamicVar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redactedrice.randomizer.lua.Issue;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRepository;

/**
 * Validates module provides/needs dynamic var metadata at load time and builds teh registry for
 * later runtime validation.
 */
public final class DynamicVarValidator {
    public static final String CATEGORY = "module dynamic vars";

    private DynamicVarValidator() {}

    public static List<Issue> validate(ModuleRepository repository, DynamicVarRegistry registry,
            List<Issue> issues) {
        if (issues == null) {
            issues = new ArrayList<>();
        }

        registry.buildFrom(repository);
        collectDuplicateProvideWarnings(repository.getAllModulesAndScripts(), issues);

        for (List<DynamicVarNeed> needs : registry.getNeedsByConsumerId().values()) {
            for (DynamicVarNeed need : needs) {
                if (need.isSatisfied()) {
                    continue;
                }

                issues.add(new Issue(need.getModule(), need.getNeed().getName(), CATEGORY, true,
                        formatMissingProviderMessage(need)));
            }
        }

        return issues;
    }

    private static void collectDuplicateProvideWarnings(List<Module> allModules,
            List<Issue> issues) {
        Map<String, DynamicVarProvide> firstProviderByName = new LinkedHashMap<>();

        for (Module module : allModules) {
            for (DynamicVar provide : module.getProvides()) {
                DynamicVarProvide provider = new DynamicVarProvide(module, provide);
                DynamicVarProvide existing = firstProviderByName.get(provide.getName());
                if (existing == null) {
                    firstProviderByName.put(provide.getName(), provider);
                    continue;
                }

                if (!existing.getDefinition().getType().equals(provide.getType())) {
                    issues.add(new Issue(module, provide.getName(), CATEGORY, false,
                            formatConflictingProvideMessage(existing, provider)));
                }
            }
        }
    }

    private static String formatConflictingProvideMessage(DynamicVarProvide existing,
            DynamicVarProvide conflicting) {
        return moduleInfoString(conflicting.getModule()) + ": provides "
                + conflicting.getDefinition() + " but " + existing.getModuleId()
                + " already provides " + existing.getDefinition().getName() + " ("
                + existing.getDefinition().getType() + ")";
    }

    private static String formatMissingProviderMessage(DynamicVarNeed need) {
        String consumerInfo = moduleInfoString(need.getModule());
        String needDescription = need.getNeed().toString();
        return consumerInfo + ": needs " + needDescription
                + " but no loaded module or script provides a compatible value";
    }

    static String formatSatisfiedNeedMessage(DynamicVarNeed need) {
        String providerIds =
                need.getCompatibleProviderModuleIds().stream().collect(Collectors.joining(", "));
        return moduleInfoString(need.getModule()) + ": needs " + need.getNeed() + " (providers: "
                + providerIds + ")";
    }

    private static String moduleInfoString(Module module) {
        String kind = module.isScript() ? "script" : "module";
        return module.getId() + " (" + module.getName() + ", " + kind + ")";
    }
}
