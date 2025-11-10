package redactedrice.randomizer.wrapper;

import redactedrice.randomizer.metadata.*;
import redactedrice.randomizer.logger.Logger;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.*;

// loads lua modules from directories and parses their metadata
public class LuaModuleLoader {
    LuaSandbox sandbox;
    Map<String, LuaModuleMetadata> modules;
    List<LuaModuleMetadata> prescripts;
    List<LuaModuleMetadata> postscripts;
    List<String> errors;

    public LuaModuleLoader(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
        this.modules = new HashMap<>();
        this.prescripts = new ArrayList<>();
        this.postscripts = new ArrayList<>();
        this.errors = new ArrayList<>();
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

        int loadedCount = 0;
        // recursively find all lua files
        List<File> luaFiles = findLuaFiles(directory);

        // load each lua file as a module
        for (File file : luaFiles) {
            try {
                LuaModuleMetadata metadata = loadModule(file);
                if (metadata != null) {
                    // store by module name so we can look it up later
                    modules.put(metadata.getName(), metadata);
                    loadedCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                addError("Error loading module from " + file.getPath() + ": " + e.getMessage());
            }
        }

        // Load the pre & post scripts as well
        loadPreScriptsFromDirectory(directoryPath);
        loadPostScriptsFromDirectory(directoryPath);

        return loadedCount;
    }

    private int loadScriptsFromSubfolder(String directoryPath, String subfolder,
            List<LuaModuleMetadata> targetList, String scriptType) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return 0;
        }

        File scriptsDir = new File(directoryPath, subfolder);
        if (!scriptsDir.exists() || !scriptsDir.isDirectory()) {
            return 0;
        }

        int loadedCount = 0;
        List<File> luaFiles = findLuaFiles(scriptsDir, false);

        for (File file : luaFiles) {
            try {
                LuaModuleMetadata metadata = loadModule(file);
                if (metadata != null) {
                    targetList.add(metadata);
                    loadedCount++;
                    Logger.info("Loaded " + scriptType + ": " + metadata.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                addError("Error loading " + scriptType + " from " + file.getPath() + ": "
                        + e.getMessage());
            }
        }

        return loadedCount;
    }

    public int loadPreScriptsFromDirectory(String directoryPath) {
        return loadScriptsFromSubfolder(directoryPath, "prescripts", prescripts, "prescript");
    }

    public int loadPostScriptsFromDirectory(String directoryPath) {
        return loadScriptsFromSubfolder(directoryPath, "postscripts", postscripts, "postscript");
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

            // get optional 'when' field for scripts (pre-randomize, pre-module, post-module,
            // post-randomize)
            String when = getStringField(moduleTable, "when", null);

            LuaModuleMetadata metadata = new LuaModuleMetadata(name, description, group, modifies,
                    arguments, executeFunction, onLoadFunction, file.getAbsolutePath(), seedOffset,
                    when);

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

    private List<File> findLuaFiles(File directory) {
        return findLuaFiles(directory, true);
    }

    private List<File> findLuaFiles(File directory, boolean excludeScriptFolders) {
        List<File> luaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Skip script folders if requested (they're loaded separately)
                    String folderName = file.getName().toLowerCase();
                    if (excludeScriptFolders && (folderName.equals("prescripts")
                            || folderName.equals("postscripts"))) {
                        continue;
                    }
                    // recurse into subdirectories
                    luaFiles.addAll(findLuaFiles(file, excludeScriptFolders));
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".lua")) {
                    luaFiles.add(file);
                }
            }
        }

        return luaFiles;
    }

    private void addError(String error) {
        errors.add(error);
        System.err.println("[LuaModuleLoader] " + error);
    }

    public LuaModuleMetadata getModule(String name) {
        return modules.get(name);
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

    public List<LuaModuleMetadata> getPreScripts() {
        return new ArrayList<>(prescripts);
    }

    public List<LuaModuleMetadata> getPostScripts() {
        return new ArrayList<>(postscripts);
    }

    public void clear() {
        modules.clear();
        prescripts.clear();
        postscripts.clear();
        errors.clear();
    }
}
