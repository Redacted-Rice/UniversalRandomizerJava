package redactedrice.randomizer;

import redactedrice.randomizer.example.ExampleEntityType;
import redactedrice.randomizer.example.ItemRarity;
import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.LuaRandomizerWrapper;
import redactedrice.randomizer.lua.ExecutionResult;
import redactedrice.randomizer.lua.ExecutionRequest;
import redactedrice.randomizer.lua.requirements.CoreRequirements;
import redactedrice.randomizer.utils.RandomizerBundledResources;
import redactedrice.randomizer.utils.LogLevel;
import redactedrice.randomizer.utils.Logger;
import redactedrice.randomizer.utils.IssueTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

// example app showing how to use the lua randomizer wrapper
public class ExampleApp {
    public static void main(String[] args) {
        if (ExampleScriptTestRunner.handles(args)) {
            System.exit(ExampleScriptTestRunner.run(args));
        }

        FileOutputStream logFileStream = null;
        FileOutputStream warnErrFileStream = null;
        try {
            // Set up file logging
            File logFile = new File("randomizer.log");
            File warnErrFile = new File("randomizer_warn_err.log");

            logFileStream = new FileOutputStream(logFile, false); // false = overwrite
            warnErrFileStream = new FileOutputStream(warnErrFile, false);

            runExample(logFileStream, warnErrFileStream, logFile, warnErrFile);

            System.out.println("\n=== Logs written to: ===");
            System.out.println("  All logs: " + logFile.getAbsolutePath());
            System.out.println("  Warnings & Errors: " + warnErrFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error setting up logging: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (logFileStream != null) {
                try {
                    logFileStream.close();
                } catch (Exception e) {
                    System.err.println("Error closing log file: " + e.getMessage());
                }
            }
            if (warnErrFileStream != null) {
                try {
                    warnErrFileStream.close();
                } catch (Exception e) {
                    System.err.println("Error closing warn/err file: " + e.getMessage());
                }
            }
        }
    }

    static void runExample(FileOutputStream logFileStream, FileOutputStream warnErrFileStream,
            File logFile, File warnErrFile) {
        System.out.println("=== Lua Randomizer Wrapper Example App ===\n");

        File appDir = new File(".");
        LuaRandomizerWrapper wrapper = createWrapper(appDir);
        System.out.println("Using bundled randomizer files from: "
                + RandomizerBundledResources.getInstalledDir(appDir).getAbsolutePath());

        // Configure log output with fine-grained control:
        // All levels to system out (default setting)
        // All levels to randomizer.log
        // Warn and Error to randomizer_warn_err.log
        Logger.setEnabled(true);
        Logger.addStreamForAllLevels(logFileStream); // All logs to main log file
        // Warnings and errors to separate file
        Logger.addStreamForLevels(warnErrFileStream, LogLevel.WARN, LogLevel.ERROR);
        Logger.setShowTimestamp(false);
        Logger.setShowModuleName(true);
        // Logger.setMinLogLevel(LogLevel.INFO);

        System.out.println("Logging configuration:");
        System.out.println("  All levels → Console + " + logFile.getName());
        System.out.println("  WARN/ERROR → " + warnErrFile.getName());

        System.out.println("Loading modules...");
        int loaded = wrapper.loadModules();
        if (IssueTracker.hasErrors()) {
            throw new IllegalStateException(
                    "Module requirement validation failed: " + IssueTracker.getErrors());
        }
        System.out.println("Loaded " + loaded + " modules\n");
        wrapper.printModuleSummary();

        // Create test entities with varied stats
        // Warriors: High health/defense, moderate damage, low speed
        // Mages: Low health/defense, high damage, moderate speed
        // Rogues: Moderate health, moderate damage, high speed, low defense
        // Clerics: High health, low damage, moderate speed/defense
        // Rangers: Moderate health, moderate damage/defense, high speed
        List<ExampleEntity> entitiesOriginal = Arrays.asList(
                new ExampleEntity("Player1", ExampleEntityType.WARRIOR, 150, 15.0, 5, 20),
                new ExampleEntity("Player2", ExampleEntityType.MAGE, 80, 35.0, 10, 5),
                new ExampleEntity("Player3", ExampleEntityType.ROGUE, 100, 20.0, 25, 8),
                new ExampleEntity("Player4", ExampleEntityType.CLERIC, 120, 12.0, 12, 15),
                new ExampleEntity("Player5", ExampleEntityType.RANGER, 110, 18.0, 22, 12),
                new ExampleEntity("Player6", ExampleEntityType.WARRIOR, 140, 18.0, 6, 22),
                new ExampleEntity("Player7", ExampleEntityType.MAGE, 75, 40.0, 8, 4),
                new ExampleEntity("Player8", ExampleEntityType.ROGUE, 90, 15.0, 20, 7));

        // Create test items with varied stats by rarity
        List<ExampleItem> itemsOriginal = Arrays.asList(
                // Common items (4 items)
                new ExampleItem("Rusty Sword", ItemRarity.COMMON, 2, 0, 5, 0),
                new ExampleItem("Leather Armor", ItemRarity.COMMON, 0, 3, 10, -1),
                new ExampleItem("Wooden Staff", ItemRarity.COMMON, 3, 1, 0, 0),
                new ExampleItem("Basic Dagger", ItemRarity.COMMON, 1, 0, 0, 2),
                // Uncommon items (3 items)
                new ExampleItem("Steel Sword", ItemRarity.UNCOMMON, 5, 2, 15, 0),
                new ExampleItem("Chain Mail", ItemRarity.UNCOMMON, 0, 8, 20, -2),
                new ExampleItem("Enchanted Bow", ItemRarity.UNCOMMON, 6, 0, 10, 3),
                // Rare items (2 items)
                new ExampleItem("Flaming Blade", ItemRarity.RARE, 12, 3, 25, 1),
                new ExampleItem("Mithril Armor", ItemRarity.RARE, 2, 15, 40, -1),
                // Legendary items (2 items)
                new ExampleItem("Excalibur", ItemRarity.LEGENDARY, 25, 8, 50, 5),
                new ExampleItem("Dragon Scale Armor", ItemRarity.LEGENDARY, 5, 30, 80, 0));

        // Create deep copies for modification
        List<ExampleEntity> entitiesModified = new ArrayList<>();
        for (ExampleEntity e : entitiesOriginal) {
            entitiesModified.add(e.copy());
        }

        List<ExampleItem> itemsModified = new ArrayList<>();
        for (ExampleItem i : itemsOriginal) {
            itemsModified.add(i.copy());
        }

        // Set up context with entities and items
        JavaContext context = new JavaContext();
        context.register("entitiesOriginal", entitiesOriginal);
        context.register("entitiesModified", entitiesModified);
        context.register("itemsOriginal", itemsOriginal);
        context.register("itemsModified", itemsModified);
        context.setConfig(JavaContext.CHANGE_DETECTION_ACTIVE, true);

        // Print original state
        System.out.println("\n=== ORIGINAL STATE ===");
        System.out.println("\nEntities:");
        for (ExampleEntity e : entitiesOriginal) {
            System.out.println("  " + e);
        }
        System.out.println("\nItems:");
        for (ExampleItem i : itemsOriginal) {
            System.out.println("  " + i);
        }

        System.out.println("\n=== EXECUTING RANDOMIZATION SCRIPTS ===\n");

        String[] scriptNames =
                {"01_shuffle_health_pool", "02_randomize_entity_types", "03_grouped_speed_by_type",
                        "04_grouped_stats_by_type", "05_shuffle_items_by_rarity",
                        "06_assign_starting_item_rarity", "07_assign_starting_item_from_rarity"};

        // create some arguements for the scripts. In a real apploication these would be provided
        // by the user or a config file or something like that

        // get SpeedClass enum values from module 3's onLoad
        EnumDefinition speedClassEnum = wrapper.getEnumDefinition("SpeedClass");

        if (speedClassEnum == null) {
            throw new IllegalStateException(
                    "SpeedClass enum not found. Make sure module 3's onLoad has registered it.");
        }

        List<String> speedClassValues = speedClassEnum.getValues();
        String SPEED_CLASS_SLOW = speedClassValues.get(0);
        String SPEED_CLASS_AVERAGE = speedClassValues.get(1);
        String SPEED_CLASS_FAST = speedClassValues.get(2);

        // Create execution requests for each module. This allows running the same module
        // more than once with different args if desired
        List<ExecutionRequest> executionRequests = new ArrayList<>();

        // Modules 1 & 2 use defaultSeedOffset from their Lua metadata and have no args
        executionRequests.add(ExecutionRequest.forModule(wrapper.getModule(scriptNames[0]), null));
        executionRequests.add(ExecutionRequest.forModule(wrapper.getModule(scriptNames[1]), null));

        // 3 requires speedByType and speedClassPools and uses a seed offset
        Map<String, Object> module3Args = new HashMap<>();
        // Map entity types to weighted list of speed classes
        Map<String, List<String>> speedByType = new HashMap<>();
        speedByType.put("WARRIOR",
                Arrays.asList(SPEED_CLASS_SLOW, SPEED_CLASS_SLOW, SPEED_CLASS_AVERAGE));
        speedByType.put("MAGE",
                Arrays.asList(SPEED_CLASS_SLOW, SPEED_CLASS_AVERAGE, SPEED_CLASS_AVERAGE));
        speedByType.put("ROGUE", Arrays.asList(SPEED_CLASS_FAST)); // Rogues are always fast
        speedByType.put("CLERIC",
                Arrays.asList(SPEED_CLASS_SLOW, SPEED_CLASS_AVERAGE, SPEED_CLASS_AVERAGE));
        speedByType.put("RANGER", Arrays.asList(SPEED_CLASS_AVERAGE, SPEED_CLASS_FAST));
        module3Args.put("speedByType", speedByType);

        // Map speed classes to speed value pools
        Map<String, List<Integer>> speedClassPools = new HashMap<>();
        speedClassPools.put(SPEED_CLASS_SLOW, Arrays.asList(5, 6, 7, 8));
        speedClassPools.put(SPEED_CLASS_AVERAGE, Arrays.asList(9, 10, 11, 12));
        speedClassPools.put(SPEED_CLASS_FAST, Arrays.asList(13, 14, 15, 16));
        module3Args.put("speedClassPools", speedClassPools);
        executionRequests
                .add(ExecutionRequest.forModuleWithSeedOffset(scriptNames[2], module3Args, 56));

        // Module 4 uses module default seed; module 5 keeps an explicit offset override. Both have
        // no args
        executionRequests.add(ExecutionRequest.forModule(wrapper.getModule(scriptNames[3]), null));
        executionRequests.add(ExecutionRequest.forModuleWithSeedOffset(scriptNames[4], null, 2));

        // 6 & 7 split starting-item assignment into rarity then item selection (module dependency
        // demo)
        Map<String, Object> module6Args = new HashMap<>();
        // COMMON: 50%, UNCOMMON: 30%, RARE: 15%, LEGENDARY: 5%
        List<ItemRarity> weightedPool = new ArrayList<>();
        for (int j = 0; j < 10; j++)
            weightedPool.add(ItemRarity.COMMON);
        for (int j = 0; j < 6; j++)
            weightedPool.add(ItemRarity.UNCOMMON);
        for (int j = 0; j < 3; j++)
            weightedPool.add(ItemRarity.RARE);
        weightedPool.add(ItemRarity.LEGENDARY);
        module6Args.put("weightedRarityPool", weightedPool);
        executionRequests
                .add(ExecutionRequest.forModule(wrapper.getModule(scriptNames[5]), module6Args));
        executionRequests.add(ExecutionRequest.forModule(wrapper.getModule(scriptNames[6]), null));

        // Execute all modules with their respective arguments. Pre and post scripts will run
        // automatically for these.
        // This does it in batch but you can also run one by one if preferred. See functional
        // tests for an example of that
        List<ExecutionResult> results = wrapper.executeModules(executionRequests, context, 12345);

        // Print the results (logs and errors)
        for (int i = 0; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            System.out.println((i + 1) + ". Executing: " + result.getModuleId());
            if (!result.isSuccess()) {
                System.err.println("   Failed: " + result.getErrorMessage());
            }
        }

        // Host popup pattern: issues accumulate across the whole batch; read once after, then clear
        if (IssueTracker.hasIssues()) {
            System.out.println("\n=== Batch Issues (popup summary) ===");
            IssueTracker.getIssues().forEach(
                    issue -> System.out.println("  [" + issue.severity() + "] " + issue.message()));
            IssueTracker.clear();
        }

        // Print modified state
        System.out.println("\n=== MODIFIED STATE ===");
        System.out.println("\nEntities:");
        for (ExampleEntity e : entitiesModified) {
            System.out.println("  " + e);
        }
        System.out.println("\nItems:");
        for (ExampleItem i : itemsModified) {
            System.out.println("  " + i);
        }
    }

    static LuaRandomizerWrapper createWrapper(File appDir) {
        File randomizerDir = RandomizerBundledResources.install(appDir, false);
        File modulesDir = new File(appDir, "lua_modules");
        CoreRequirements requirements = new CoreRequirements();
        requirements.addCoreRequirement(ExampleAppVersion.PLATFORM_KEY, ExampleAppVersion.VERSION,
                true);
        UniversalRandomizerVersions.addTo(requirements);
        LuaRandomizerWrapper wrapper =
                LuaRandomizerWrapper.forApp(randomizerDir, modulesDir, requirements);
        registerSharedEnums(wrapper);
        return wrapper;
    }

    static void registerSharedEnums(LuaRandomizerWrapper wrapper) {
        wrapper.registerSharedEnum("EE_EntityTypes", ExampleEntityType.class);
        wrapper.registerSharedEnum("ItemRarity", ItemRarity.class);
    }
}
