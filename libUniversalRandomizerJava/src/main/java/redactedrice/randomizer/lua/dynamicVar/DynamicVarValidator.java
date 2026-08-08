package redactedrice.randomizer.lua.dynamicVar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import redactedrice.randomizer.lua.ExecutionPlan;
import redactedrice.randomizer.lua.Issue;
import redactedrice.randomizer.lua.Module;
import redactedrice.randomizer.lua.ModuleRepository;

/**
 * Validates module provides/needs metadata at load time and checks execution
 * order before a batch
 * runs. Does not inspect or enforce actual Lua context values. modules own
 * reading and writing
 * those values. validation only checks that declared names and types can be
 * satisfied in order.
 * Type names are compared case insensitively. A module cannot satisfy its own
 * needs.
 */
public final class DynamicVarValidator {
    public static final String CATEGORY = "module dynamic vars";
    public static final String EXECUTION_CATEGORY = "module dynamic vars (execution order)";

    private DynamicVarValidator() {
    }

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

    public static List<Issue> validateExecutionPlan(ExecutionPlan plan, List<Issue> issues) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        if (plan == null) {
            return issues;
        }

        List<Module> steps = plan.getSteps();
        Map<String, DynamicVar> provided = new LinkedHashMap<>();

        for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
            Module step = steps.get(stepIndex);
            if (step == null) {
                continue;
            }

            for (DynamicVar need : step.getNeeds()) {
                DynamicVar available = provided.get(need.getName());
                if (available != null && available.satisfiesNeed(need)) {
                    continue;
                }

                issues.add(new Issue(step, need.getName(), EXECUTION_CATEGORY, true,
                        formatExecutionOrderMessage(step, need, stepIndex, steps, provided)));
            }

            for (DynamicVar provide : step.getProvides()) {
                provided.put(provide.getName(), provide);
            }
        }

        return issues;
    }

    private static String formatExecutionOrderMessage(Module consumer, DynamicVar need,
            int consumerIndex, List<Module> steps, Map<String, DynamicVar> provided) {
        String consumerInfo = moduleInfoString(consumer);
        DynamicVar available = provided.get(need.getName());
        if (available != null && !available.satisfiesNeed(need)) {
            return consumerInfo + ": needs " + need + " but earlier step provides incompatible "
                    + available;
        }

        List<String> laterProviderIds = new ArrayList<>();
        for (int i = consumerIndex + 1; i < steps.size(); i++) {
            Module step = steps.get(i);
            if (step == null) {
                continue;
            }
            for (DynamicVar provide : step.getProvides()) {
                if (provide.satisfiesNeed(need)) {
                    laterProviderIds.add(step.getId());
                }
            }
        }

        if (!laterProviderIds.isEmpty()) {
            return consumerInfo + ": needs " + need + " but compatible provider(s) run later in "
                    + "the plan: " + String.join(", ", laterProviderIds);
        }

        return consumerInfo + ": needs " + need
                + " but no earlier step in the execution plan provides a compatible value";
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

                if (!existing.getDefinition().typesMatch(provide.getType())) {
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

    private static String moduleInfoString(Module module) {
        String kind = module.isScript() ? "script" : "module";
        return module.getId() + " (" + module.getName() + ", " + kind + ")";
    }
}
