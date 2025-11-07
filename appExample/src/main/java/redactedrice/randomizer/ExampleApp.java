package redactedrice.randomizer;

import redactedrice.randomizer.context.EnumDefinition;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.context.PseudoEnumRegistry;
import redactedrice.randomizer.wrapper.LuaRandomizerWrapper;
import redactedrice.randomizer.wrapper.ExecutionResult;
import redactedrice.randomizer.wrapper.RandomizerResourceExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

// example app showing how to use the lua randomizer wrapper
public class ExampleApp {
    public static void main(String[] args) {
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

        // Set the extraction path for bundled randomizer files
        // Default is "randomizer" if not set
        String randomizerExtractionPath = new File("randomizer").getAbsolutePath();
        RandomizerResourceExtractor.setPath(randomizerExtractionPath);
        String modulesPath = new File("lua_modules").getAbsolutePath();

        PseudoEnumRegistry pseudoEnums = new PseudoEnumRegistry();
        pseudoEnums.registerEnum("ModuleGroup", "players", "enemies", "utils");
        pseudoEnums.registerEnum("ModuleModifies", "name", "health", "damage", "speed", "defense",
                "type", "startingItem");

        // Extract bundled randomizer file
        try {
            // Overwrite existing files. Normally I would probably not do this so the files can be modified
            // if desired but for the example I do this to ensure it picks up any updates from the
            // universal randomizer core
            RandomizerResourceExtractor.extract(true);
            System.out.println("Using bundled randomizer files from: " + randomizerExtractionPath);
        } catch (Exception e) {
            System.out.println(
                    "Failed to extract core lua randomizer files. Error: " + e.getMessage());
        }

        LuaRandomizerWrapper wrapper =
                new LuaRandomizerWrapper(Collections.singletonList(modulesPath), pseudoEnums);

        // Configure log output with fine-grained control:
        // All levels to system out (default setting)
        // All levels to randomizer.log
        // Warn and Error to randomizer_warn_err.log
        wrapper.setLogEnabled(true);
        wrapper.addStreamForAllLogLevels(logFileStream); // All logs to main log file
        wrapper.addStreamForLogLevels(warnErrFileStream,
                redactedrice.randomizer.logger.Logger.LogLevel.WARN,
                redactedrice.randomizer.logger.Logger.LogLevel.ERROR); // Warnings and errors to
                                                                       // separate file
        wrapper.setShowLogTimestamp(false);
        wrapper.setShowLogModuleName(true);
        // wrapper.setLogMinLevel(redactedrice.randomizer.debug.Logger.LogLevel.INFO);

        System.out.println("Logging configuration:");
        System.out.println("  All levels → Console + " + logFile.getName());
        System.out.println("  WARN/ERROR → " + warnErrFile.getName());

        System.out.println("Loading modules...");
        int loaded = wrapper.loadModules();
        System.out.println("Loaded " + loaded + " modules\n");
        wrapper.printModuleSummary();
        wrapper.setChangeDetectionEnabled(true);

        // Create test entities with varied stats
        // Warriors: High health/defense, moderate damage, low speed
        // Mages: Low health/defense, high damage, moderate speed
        // Rogues: Moderate health, moderate damage, high speed, low defense
        // Clerics: High health, low damage, moderate speed/defense
        // Rangers: Moderate health, moderate damage/defense, high speed
        List<ExampleEntity> entitiesOriginal = Arrays.asList(
                new ExampleEntity("Player1", ExampleEntity.EntityType.WARRIOR, 150, 15.0, 5, 20),
                new ExampleEntity("Player2", ExampleEntity.EntityType.MAGE, 80, 35.0, 10, 5),
                new ExampleEntity("Player3", ExampleEntity.EntityType.ROGUE, 100, 20.0, 25, 8),
                new ExampleEntity("Player4", ExampleEntity.EntityType.CLERIC, 120, 12.0, 12, 15),
                new ExampleEntity("Player5", ExampleEntity.EntityType.RANGER, 110, 18.0, 22, 12),
                new ExampleEntity("Player6", ExampleEntity.EntityType.WARRIOR, 140, 18.0, 6, 22),
                new ExampleEntity("Player7", ExampleEntity.EntityType.MAGE, 75, 40.0, 8, 4),
                new ExampleEntity("Player8", ExampleEntity.EntityType.ROGUE, 90, 15.0, 20, 7));

        // Create test items with varied stats by rarity
        List<ExampleItem> itemsOriginal = Arrays.asList(
                // Common items (4 items)
                new ExampleItem("Rusty Sword", ExampleItem.ItemRarity.COMMON, 2, 0, 5, 0),
                new ExampleItem("Leather Armor", ExampleItem.ItemRarity.COMMON, 0, 3, 10, -1),
                new ExampleItem("Wooden Staff", ExampleItem.ItemRarity.COMMON, 3, 1, 0, 0),
                new ExampleItem("Basic Dagger", ExampleItem.ItemRarity.COMMON, 1, 0, 0, 2),
                // Uncommon items (3 items)
                new ExampleItem("Steel Sword", ExampleItem.ItemRarity.UNCOMMON, 5, 2, 15, 0),
                new ExampleItem("Chain Mail", ExampleItem.ItemRarity.UNCOMMON, 0, 8, 20, -2),
                new ExampleItem("Enchanted Bow", ExampleItem.ItemRarity.UNCOMMON, 6, 0, 10, 3),
                // Rare items (2 items)
                new ExampleItem("Flaming Blade", ExampleItem.ItemRarity.RARE, 12, 3, 25, 1),
                new ExampleItem("Mithril Armor", ExampleItem.ItemRarity.RARE, 2, 15, 40, -1),
                // Legendary items (2 items)
                new ExampleItem("Excalibur", ExampleItem.ItemRarity.LEGENDARY, 25, 8, 50, 5),
                new ExampleItem("Dragon Scale Armor", ExampleItem.ItemRarity.LEGENDARY, 5, 30, 80,
                        0));

        // Create deep copies for modification
        List<ExampleEntity> entitiesModified = new ArrayList<>();
        for (ExampleEntity e : entitiesOriginal) {
            entitiesModified.add(e.copy());
        }

        List<ExampleItem> itemsModified = new ArrayList<>();
        for (ExampleItem i : itemsOriginal) {
            itemsModified.add(i.copy());
        }

        // Set up context with entities items and enums
        JavaContext context = new JavaContext();
        context.register("entitiesOriginal", entitiesOriginal);
        context.register("entitiesModified", entitiesModified);
        context.register("itemsOriginal", itemsOriginal);
        context.register("itemsModified", itemsModified);
        // Register enums with custom names to be used in Lua context
        context.registerEnum("EE_EntityTypes", ExampleEntity.EntityType.class);
        context.registerEnum("ItemRarity", ExampleItem.ItemRarity.class);

        wrapper.setMonitoredObjects(entitiesModified.toArray(), itemsModified.toArray());

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

        String[] scriptNames = {"01_shuffle_health_pool", "02_randomize_entity_types",
                "03_grouped_speed_by_type", "04_grouped_stats_by_type",
                "05_shuffle_items_by_rarity", "06_assign_starting_items"};

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

        // Prepare arguments for each module upfront
        List<Map<String, Object>> moduleArguments = new ArrayList<>();
        // 1 & 2 have no args
        moduleArguments.add(new HashMap<>());
        moduleArguments.add(new HashMap<>());

        // 3 requires speedByType and speedClassPools
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
        moduleArguments.add(module3Args);

        // 4 & 5 have no args
        moduleArguments.add(new HashMap<>());
        moduleArguments.add(new HashMap<>());

        // 6 requires weightedRarityPool
        Map<String, Object> module6Args = new HashMap<>();
        // Create weighted rarity pool
        // COMMON: 50% chance (10 out of 20 entries)
        // UNCOMMON: 30% chance (6 out of 20 entries)
        // RARE: 15% chance (3 out of 20 entries)
        // LEGENDARY: 5% chance (1 out of 20 entries)
        List<ExampleItem.ItemRarity> weightedPool = new ArrayList<>();
        for (int j = 0; j < 10; j++)
            weightedPool.add(ExampleItem.ItemRarity.COMMON);
        for (int j = 0; j < 6; j++)
            weightedPool.add(ExampleItem.ItemRarity.UNCOMMON);
        for (int j = 0; j < 3; j++)
            weightedPool.add(ExampleItem.ItemRarity.RARE);
        weightedPool.add(ExampleItem.ItemRarity.LEGENDARY);
        module6Args.put("weightedRarityPool", weightedPool);
        moduleArguments.add(module6Args);

        // Execute all modules with their respective arguments
        for (int i = 0; i < scriptNames.length; i++) {
            System.out.println((i + 1) + ". Executing: " + scriptNames[i]);

            ExecutionResult result = wrapper.executeModule(scriptNames[i], context,
                    moduleArguments.get(i), 12345 + i);
            if (!result.isSuccess()) {
                System.err.println("   Failed: " + result.getErrorMessage());
            }
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
}
