package redactedrice.randomizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.example.ExampleEntityType;
import redactedrice.randomizer.example.ItemRarity;
import redactedrice.randomizer.lua.requirements.CoreRequirements;
import redactedrice.randomizer.scripttests.ScriptTestCase;
import redactedrice.randomizer.scripttests.ScriptTestCli;
import redactedrice.randomizer.scripttests.ScriptTestFields;
import redactedrice.randomizer.scripttests.ScriptTestFixtures;
import redactedrice.randomizer.scripttests.ScriptTestValues;
import redactedrice.randomizer.utils.IssueTracker;
import redactedrice.randomizer.utils.RandomizerBundledResources;

// Example host for URJ script tests. Cases use entities or items. Scripts still get
// original/modified copies because that is how this app's modules read data.
public final class ExampleScriptTestRunner {
    public static final String SCRIPT_TESTS_DIR_NAME = "script_tests";

    private ExampleScriptTestRunner() {}

    public static boolean handles(String[] args) {
        return ScriptTestCli.handles(args);
    }

    public static int run(String[] args) {
        File appDir = new File(".").getAbsoluteFile();
        File testsDir = new File(appDir, SCRIPT_TESTS_DIR_NAME);
        return ScriptTestCli.run(args, testsDir.toPath(), loadWrapper(appDir), new Fixtures());
    }

    private static LuaRandomizerWrapper loadWrapper(File appDir) {
        File randomizerDir = RandomizerBundledResources.install(appDir, true);
        File modulesDir = new File(appDir, "lua_modules");
        if (!modulesDir.isDirectory()) {
            throw new IllegalStateException(
                    "Action modules dir is missing: " + modulesDir.getAbsolutePath());
        }

        List<String> allowedDirectories = new ArrayList<>();
        allowedDirectories.add(randomizerDir.getAbsolutePath());
        allowedDirectories.add(modulesDir.getAbsolutePath());

        CoreRequirements requirements = new CoreRequirements();
        requirements.addCoreRequirement(ExampleAppVersion.PLATFORM_KEY, ExampleAppVersion.VERSION,
                true);
        UniversalRandomizerVersions.addTo(requirements);

        LuaRandomizerWrapper wrapper = new LuaRandomizerWrapper(allowedDirectories,
                List.of(modulesDir.getAbsolutePath()), null, requirements);

        IssueTracker.clear();
        int loaded = wrapper.loadModules();
        if (loaded <= 0) {
            throw new IllegalStateException(
                    "No action modules loaded from " + modulesDir.getAbsolutePath());
        }
        if (IssueTracker.hasErrors()) {
            throw new IllegalStateException("Module load failed: " + IssueTracker.getErrors());
        }
        return wrapper;
    }

    private static final class Fixtures implements ScriptTestFixtures {
        @Override
        public void populateContext(JavaContext context, ScriptTestCase testCase) {
            context.registerEnum("EE_EntityTypes", ExampleEntityType.class);
            context.registerEnum("ItemRarity", ItemRarity.class);
            context.setConfig("changeDetectionActive", true);

            Map<String, Object> data = testCase.data();
            List<Map<String, Object>> entitySpecs = specs(data, "entities");
            List<Map<String, Object>> itemSpecs = specs(data, "items");
            if (entitySpecs.isEmpty() && itemSpecs.isEmpty()) {
                throw new IllegalArgumentException(
                        testCase.displayName() + " needs an entities list or an items list");
            }

            // Same starting values on both sides. Modules that shuffle from original still work.
            context.register("entitiesOriginal", buildEntities(context, entitySpecs));
            context.register("entitiesModified", buildEntities(context, entitySpecs));
            context.register("itemsOriginal", buildItems(context, itemSpecs));
            context.register("itemsModified", buildItems(context, itemSpecs));
        }

        @Override
        public void assertExpect(ScriptTestCase testCase, JavaContext context) {
            String label = testCase.displayName();
            List<Map<String, Object>> expect =
                    ScriptTestValues.listOfMaps(testCase.data().get("expect"), "expect");
            @SuppressWarnings("unchecked")
            List<ExampleEntity> entities = (List<ExampleEntity>) context.get("entitiesModified");
            @SuppressWarnings("unchecked")
            List<ExampleItem> items = (List<ExampleItem>) context.get("itemsModified");
            List<?> actuals = !entities.isEmpty() ? entities : items;
            String kind = !entities.isEmpty() ? "entity" : "item";

            List<String> mismatches = new ArrayList<>();
            for (Map<String, Object> expected : expect) {
                String name = ScriptTestValues.requiredString(expected, "name");
                Object match = findByName(actuals, name);
                if (match == null) {
                    mismatches.add("expected " + kind + " '" + name + "' but it was not in the list");
                    continue;
                }
                ScriptTestFields.collectMismatches(context, match, expected, mismatches,
                        kind + " '" + name + "'");
            }
            if (!mismatches.isEmpty()) {
                throw new IllegalStateException(label + " " + String.join(". ", mismatches));
            }
        }

        private static List<Map<String, Object>> specs(Map<String, Object> data, String field) {
            Object value = data.get(field);
            if (value == null) {
                return List.of();
            }
            return ScriptTestValues.listOfMaps(value, field);
        }

        private static List<ExampleEntity> buildEntities(JavaContext context,
                List<Map<String, Object>> specs) {
            List<ExampleEntity> entities = new ArrayList<>();
            for (Map<String, Object> spec : specs) {
                ExampleEntity entity =
                        new ExampleEntity("Unnamed", ExampleEntityType.WARRIOR, 100, 10.0, 10, 10);
                ScriptTestFields.apply(context, entity, spec);
                if (entity.getName() == null || entity.getName().isBlank()) {
                    throw new IllegalArgumentException("Entity spec needs a name");
                }
                entities.add(entity);
            }
            return entities;
        }

        private static List<ExampleItem> buildItems(JavaContext context,
                List<Map<String, Object>> specs) {
            List<ExampleItem> items = new ArrayList<>();
            for (Map<String, Object> spec : specs) {
                ExampleItem item = new ExampleItem("Unnamed", ItemRarity.COMMON, 0, 0, 0, 0);
                ScriptTestFields.apply(context, item, spec);
                if (item.name == null || item.name.isBlank()) {
                    throw new IllegalArgumentException("Item spec needs a name");
                }
                items.add(item);
            }
            return items;
        }

        private static Object findByName(List<?> actuals, String name) {
            for (Object actual : actuals) {
                if (actual instanceof ExampleEntity entity && name.equals(entity.getName())) {
                    return entity;
                }
                if (actual instanceof ExampleItem item && name.equals(item.name)) {
                    return item;
                }
            }
            return null;
        }
    }
}
