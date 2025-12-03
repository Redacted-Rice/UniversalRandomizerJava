package redactedrice.randomizer.wrapper;

import redactedrice.randomizer.metadata.*;
import redactedrice.randomizer.logger.Logger;
import redactedrice.randomizer.wrapper.sandbox.LuaSandbox;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.*;

// loads lua modules from directories, parses their metadata, and stores them providing lookup
// mechanisms for them/their metadata
public class ModuleRegistry {
    LuaSandbox sandbox;
    // Modules are the core randomization that are manually specified and run
    // This is a map from the module name to its metadata
    Map<String, LuaModuleMetadata> modules;
    // Modules organized by their group metadata field
    Map<String, List<LuaModuleMetadata>> modulesByGroup;
    // Scripts are automatically run before and after triggers. Name may change
    Map<String, Map<String, List<LuaModuleMetadata>>> scriptsByType;
    List<String> errors;

    public static final String SCRIPT_TIMING_PRE = "pre";
    public static final String SCRIPT_TIMING_POST = "post";

    public static final String SCRIPT_WHEN_RANDOMIZE = "randomize";
    public static final String SCRIPT_WHEN_MODULE = "module";

    public ModuleRegistry(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
        this.modules = new HashMap<>();
        this.modulesByGroup = new HashMap<>();
        this.scriptsByType = new HashMap<>();
        this.errors = new ArrayList<>();

        // Initialize the scripts maps
        Map<String, List<LuaModuleMetadata>> preScripts = new HashMap<>();
        preScripts.put(SCRIPT_WHEN_RANDOMIZE, new ArrayList<>());
        preScripts.put(SCRIPT_WHEN_MODULE, new ArrayList<>());
        scriptsByType.put(SCRIPT_TIMING_PRE, preScripts);

        Map<String, List<LuaModuleMetadata>> postScripts = new HashMap<>();
        postScripts.put(SCRIPT_WHEN_RANDOMIZE, new ArrayList<>());
        postScripts.put(SCRIPT_WHEN_MODULE, new ArrayList<>());
        scriptsByType.put(SCRIPT_TIMING_POST, postScripts);
    }

    public int loadModulesFromDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            addError("Directory path cannot be null or empty");
            return 0;
        }

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            addError("Directory does not exist: " + directoryPath);
            return 0;
        }

        if (!directory.isDirectory()) {
            addError("Path is not a directory: " + directoryPath);
            return 0;
        }

        // Load modules from actions subfolder
        int loadedCount = loadModulesFromSubfolder(directoryPath);

        // Load the pre & post scripts as well
        loadPreScriptsFromDirectory(directoryPath);
        loadPostScriptsFromDirectory(directoryPath);

        return loadedCount;
    }


    private List<File> getScriptsFromSubdirectory(String directoryPath, String subfolder) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return new ArrayList<>();
        }

        File targetDir;
        if (subfolder == null || subfolder.trim().isEmpty()) {
            targetDir = new File(directoryPath);
        } else {
            targetDir = new File(directoryPath, subfolder);
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return new ArrayList<>();
        }

        return findLuaFiles(targetDir);
    }

    private int loadModulesFromSubfolder(String directoryPath) {
        List<File> luaFiles = getScriptsFromSubdirectory(directoryPath, "actions");
        return loadModulesFromScripts(luaFiles, modules, "module", (metadata) -> {
            modules.put(metadata.getName(), metadata);

            // Add by group as well
            String group = metadata.getGroup();
            if (group != null && !group.trim().isEmpty()) {
                modulesByGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(metadata);
            }
        });
    }

    private int loadScriptsFromSubfolder(String directoryPath, String subfolder, String timing) {
        List<File> luaFiles = getScriptsFromSubdirectory(directoryPath, subfolder);
        return loadModulesFromScripts(luaFiles, null, directoryPath, (metadata) -> {
            // Determine the when it should be run
            String when = metadata.getWhen();
            String whenKey;

            if (when != null && when.equals(SCRIPT_WHEN_MODULE)) {
                whenKey = SCRIPT_WHEN_MODULE;
            } else {
                // Default to randomize
                whenKey = SCRIPT_WHEN_RANDOMIZE;
            }

            // Add to the appropriate list in the nested map
            scriptsByType.get(timing).get(whenKey).add(metadata);
        });
    }

    private int loadModulesFromScripts(List<File> luaFiles, Object targetCollection,
            String scriptType, java.util.function.Consumer<LuaModuleMetadata> onSuccess) {
        int loadedCount = 0;

        for (File file : luaFiles) {
            try {
                LuaModuleMetadata metadata = loadModule(file);
                if (metadata != null) {
                    onSuccess.accept(metadata);
                    loadedCount++;
                    Logger.info("Loaded from " + scriptType + ": " + metadata.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                addError("Error loading script from " + file.getPath() + ": " + e.getMessage());
            }
        }

        return loadedCount;
    }

    public int loadPreScriptsFromDirectory(String directoryPath) {
        return loadScriptsFromSubfolder(directoryPath, "prescripts", SCRIPT_TIMING_PRE);
    }

    public int loadPostScriptsFromDirectory(String directoryPath) {
        return loadScriptsFromSubfolder(directoryPath, "postscripts", SCRIPT_TIMING_POST);
    }

    private LuaModuleMetadata loadModule(File file) {
        Logger.info("Loading module: " + file.getName());

        try {
            // run the lua file to get the module table back
            LuaValue result = sandbox.executeFile(file.getAbsolutePath());

            // module must return a table with its metadata
            if (!result.istable()) {
                addError(
                        file.getName() + " did not return a table (got " + result.typename() + ")");
                return null;
            }

            LuaTable moduleTable = result.checktable();

            // get required name field
            String name = getStringField(moduleTable, "name", null);
            if (name == null || name.trim().isEmpty()) {
                addError(file.getName() + " missing required 'name' field");
                return null;
            }

            String description = getStringField(moduleTable, "description", "");

            // get group or default to utility later
            String group = getStringField(moduleTable, "group", null);

            // parse modifies field which can be string or table
            List<String> modifies = new ArrayList<>();
            LuaValue modifiesValue = moduleTable.get("modifies");
            if (!modifiesValue.isnil()) {
                if (modifiesValue.istable()) {
                    // its a table so iterate through all values
                    LuaTable modifiesTable = modifiesValue.checktable();
                    LuaValue key = LuaValue.NIL;
                    while (true) {
                        key = modifiesTable.next(key).arg1();
                        if (key.isnil()) {
                            break;
                        }
                        LuaValue value = modifiesTable.get(key);
                        if (value.isstring()) {
                            modifies.add(value.tojstring());
                        }
                    }
                } else if (modifiesValue.isstring()) {
                    // single string works too
                    modifies.add(modifiesValue.tojstring());
                }
            }

            // get seed offset or use 0
            int seedOffset = (int) getNumberField(moduleTable, "seedOffset", 0);

            // get required execute function
            LuaValue executeValue = moduleTable.get("execute");
            if (!executeValue.isfunction()) {
                addError(file.getName() + " missing required 'execute' function");
                return null;
            }
            LuaFunction executeFunction = executeValue.checkfunction();

            // get optional onload function
            LuaFunction onLoadFunction = null;
            LuaValue onLoadValue = moduleTable.get("onLoad");
            if (!onLoadValue.isnil() && onLoadValue.isfunction()) {
                onLoadFunction = onLoadValue.checkfunction();
            }

            // parse arguments table if present
            List<ArgumentDefinition> arguments = new ArrayList<>();
            LuaValue argsValue = moduleTable.get("arguments");
            if (!argsValue.isnil()) {
                if (!argsValue.istable()) {
                    addError(file.getName() + " 'arguments' field must be a table");
                    return null;
                }

                LuaTable argsTable = argsValue.checktable();
                arguments = parseArguments(argsTable, file.getName());
            }

            // get optional 'when' field for scripts (randomize & module)
            String when = getStringField(moduleTable, "when", null);

            // get required details fields
            String author = getStringField(moduleTable, "author", null);
            if (author == null || author.trim().isEmpty()) {
                addError(file.getName() + " missing required 'author' field");
                return null;
            }

            String version = getStringField(moduleTable, "version", null);
            if (version == null || version.trim().isEmpty()) {
                addError(file.getName() + " missing required 'version' field");
                return null;
            }

            // parse requires map
            Map<String, String> requires = parseRequiresMap(moduleTable, file.getName());
            if (requires == null || requires.isEmpty()) {
                addError(file.getName() + " missing required 'requires' table or it is empty");
                return null;
            }
            if (!requires.containsKey("UniversalRandomizerJava")) {
                addError(file.getName()
                        + " 'requires' field must contain 'UniversalRandomizerJava'");
                return null;
            }

            // Get optional info fields
            String source = getStringField(moduleTable, "source", null);
            String license = getStringField(moduleTable, "license", null);
            String about = getStringField(moduleTable, "about", null);

            LuaModuleMetadata metadata = new LuaModuleMetadata(name, description, group, modifies,
                    arguments, executeFunction, onLoadFunction, file.getAbsolutePath(), seedOffset,
                    when, author, version, requires, source, license, about);

            Logger.info("Finished loading module: " + name);

            return metadata;

        } catch (LuaError e) {
            addError("Lua error in " + file.getName() + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            addError("Error loading " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private List<ArgumentDefinition> parseArguments(LuaTable argsTable, String fileName) {
        List<ArgumentDefinition> arguments = new ArrayList<>();

        // walk through the array part of the lua table
        LuaValue key = LuaValue.NIL;
        while (true) {
            key = argsTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }

            LuaValue argValue = argsTable.get(key);
            if (!argValue.istable()) {
                addError(fileName + " argument entry must be a table");
                continue;
            }

            // parse each argument definition
            try {
                ArgumentDefinition argDef =
                        parseArgumentDefinition(argValue.checktable(), fileName);
                if (argDef != null) {
                    arguments.add(argDef);
                }
            } catch (Exception e) {
                addError(fileName + " error parsing argument: " + e.getMessage());
            }
        }

        return arguments;
    }

    private ArgumentDefinition parseArgumentDefinition(LuaTable argTable, String fileName) {
        String name = getStringField(argTable, "name", null);
        if (name == null || name.trim().isEmpty()) {
            addError(fileName + " argument missing 'name' field");
            return null;
        }

        // get the type definition which can be string or table
        LuaValue definitionValue = argTable.get("definition");
        if (definitionValue.isnil()) {
            addError(fileName + " argument '" + name + "' missing 'definition' field");
            return null;
        }

        TypeDefinition typeDef;
        try {
            if (definitionValue.isstring()) {
                // simple type like "number" or "string"
                typeDef = TypeDefinition.parse(definitionValue.tojstring());
            } else if (definitionValue.istable()) {
                // complex type with constraints embedded
                typeDef = TypeDefinition.parse(luaTableToMap(definitionValue.checktable()));
            } else {
                addError(fileName + " argument '" + name + "' has invalid definition field");
                return null;
            }
        } catch (IllegalArgumentException e) {
            addError(fileName + " invalid argument definition: " + e.getMessage());
            return null;
        }

        // get default value if present
        LuaValue defaultValue = argTable.get("default");
        Object javaDefaultValue = null;
        if (!defaultValue.isnil()) {
            javaDefaultValue = luaValueToJava(defaultValue);
        }

        return new ArgumentDefinition(name, typeDef, javaDefaultValue);
    }

    private Map<String, Object> luaTableToMap(LuaTable table) {
        Map<String, Object> map = new HashMap<>();
        LuaValue[] keys = table.keys();
        for (LuaValue key : keys) {
            if (key.isstring()) {
                LuaValue value = table.get(key);
                map.put(key.tojstring(), luaValueToJava(value));
            }
        }
        return map;
    }

    private Object luaValueToJava(LuaValue value) {
        if (value.isnil()) {
            return null;
        } else if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isint()) {
            return value.toint();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            LuaTable table = value.checktable();
            // check if its an array or a map
            if (isLuaArray(table)) {
                return luaTableToList(table);
            } else {
                return luaTableToMap(table);
            }
        }
        return value.toString();
    }

    private boolean isLuaArray(LuaTable table) {
        int length = table.length();
        if (length == 0) {
            return false;
        }
        // lua arrays have sequential keys from 1 to n
        for (int i = 1; i <= length; i++) {
            if (table.get(i).isnil()) {
                return false;
            }
        }
        return true;
    }

    private List<Object> luaTableToList(LuaTable table) {
        List<Object> list = new ArrayList<>();
        int length = table.length();
        for (int i = 1; i <= length; i++) {
            list.add(luaValueToJava(table.get(i)));
        }
        return list;
    }

    private String getStringField(LuaTable table, String fieldName, String defaultValue) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return defaultValue;
        }
        return value.tojstring();
    }

    private double getNumberField(LuaTable table, String fieldName, double defaultValue) {
        LuaValue value = table.get(fieldName);
        if (value.isnil()) {
            return defaultValue;
        }
        if (value.isnumber()) {
            return value.todouble();
        }
        return defaultValue;
    }

    private Map<String, String> parseRequiresMap(LuaTable moduleTable, String fileName) {
        LuaValue requiresValue = moduleTable.get("requires");
        if (requiresValue.isnil()) {
            return null;
        }

        if (!requiresValue.istable()) {
            addError(fileName + " 'requires' field must be a table");
            return null;
        }

        LuaTable requiresTable = requiresValue.checktable();
        Map<String, String> requires = new HashMap<>();

        // Iterate through the table
        LuaValue key = LuaValue.NIL;
        while (true) {
            key = requiresTable.next(key).arg1();
            if (key.isnil()) {
                break;
            }
            LuaValue value = requiresTable.get(key);

            if (key.isstring() && value.isstring()) {
                requires.put(key.tojstring(), value.tojstring());
            } else {
                addError(fileName + " 'requires' table must contain string keys and string values");
                return null;
            }
        }

        return requires;
    }

    private List<File> findLuaFiles(File directory) {
        List<File> luaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // recurse into subdirectories
                    luaFiles.addAll(findLuaFiles(file));
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".lua")) {
                    luaFiles.add(file);
                }
            }
        }

        return luaFiles;
    }

    private void addError(String error) {
        errors.add(error);
        System.err.println("[ModuleRegistry] " + error);
    }

    public LuaModuleMetadata getModule(String name) {
        return modules.get(name);
    }

    public Set<String> getGroups() {
        return new HashSet<>(modulesByGroup.keySet());
    }

    public List<LuaModuleMetadata> getModulesByGroup(String group) {
        if (group == null || group.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<LuaModuleMetadata> groupModules = modulesByGroup.get(group);
        return groupModules != null ? new ArrayList<>(groupModules) : new ArrayList<>();
    }

    public List<LuaModuleMetadata> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    public Set<String> getModuleNames() {
        return new HashSet<>(modules.keySet());
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<LuaModuleMetadata> getScripts(String timing, String when) {
        Map<String, List<LuaModuleMetadata>> timingMap = scriptsByType.get(timing);
        if (timingMap == null) {
            return new ArrayList<>();
        }

        List<LuaModuleMetadata> scripts = timingMap.get(when);
        return scripts != null ? new ArrayList<>(scripts) : new ArrayList<>();
    }

    public void clear() {
        modules.clear();
        modulesByGroup.clear();
        for (Map<String, List<LuaModuleMetadata>> timingMap : scriptsByType.values()) {
            for (List<LuaModuleMetadata> scripts : timingMap.values()) {
                scripts.clear();
            }
        }
        errors.clear();
    }
}
