package redactedrice.randomizer.wrapper;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.logger.Logger;
import redactedrice.randomizer.metadata.ArgumentDefinition;
import redactedrice.randomizer.metadata.LuaModuleMetadata;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.*;

// runs lua modules with the given context and arguments
public class ModuleExecutor {
    LuaSandbox sandbox;
    List<String> errors;
    List<ExecutionResult> results;

    public ModuleExecutor(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
        this.errors = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    private ExecutionResult executeLuaModule(ExecutionRequest request, LuaModuleMetadata metadata,
            JavaContext context) {
        if (metadata == null) {
            throw new IllegalArgumentException("Module metadata cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("ExecutionRequest cannot be null");
        }

        String moduleName = metadata.getName();
        String previousModuleName = Logger.getCurrentModuleName();
        ExecutionResult execResult = null;

        Logger.setCurrentModuleName(moduleName);
        try {
            // validate and convert arguements using enum context from javacontext
            Map<String, Object> validatedArgs =
                    validateArguments(metadata, request.getArguments(), context);

            logExecutionInfo(moduleName, request.getSeed(), validatedArgs, null, null);

            // set seed and get the args
            setSeedInLua(request.getSeed());
            LuaTable argsTable = convertArgumentsToLuaTable(metadata, validatedArgs);

            // Execute and return the results
            LuaValue result = executeWithTraceback(metadata, context.toLuaTable(), argsTable);
            execResult = ExecutionResult.success(request, result);
        } catch (LuaError e) {
            String errorMsg = formatLuaError(metadata, e);
            addError(errorMsg, "Lua execution error: " + e.getMessage());
            execResult = ExecutionResult.failure(request, errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = formatJavaError(metadata, e);
            addError(errorMsg, "Java exception: " + e.getMessage());
            execResult = ExecutionResult.failure(request, errorMsg);
        } finally {
            // Always set the module name back to support recursive calls
            Logger.setCurrentModuleName(previousModuleName);
        }
        results.add(execResult);
        return execResult;
    }

    private ExecutionResult executeLuaScript(LuaModuleMetadata script, JavaContext context,
            String scriptTiming, String scriptWhen) {
        if (script == null) {
            throw new IllegalArgumentException("Script metadata cannot be null");
        }
        if (scriptTiming == null || scriptWhen == null) {
            throw new IllegalArgumentException("Script timing and when cannot be null");
        }

        String moduleName = script.getName();
        String previousModuleName = Logger.getCurrentModuleName();
        ExecutionResult execResult = null;

        Logger.setCurrentModuleName(moduleName);
        try {
            // Scripts are much simpler. No args and no seed. Just log and execute
            logExecutionInfo(moduleName, 0, null, scriptTiming, scriptWhen);
            LuaValue result = executeWithTraceback(script, context.toLuaTable(), new LuaTable());
            execResult = ExecutionResult.scriptSuccess(moduleName, result);
        } catch (LuaError e) {
            String errorMsg = formatLuaError(script, e);
            addError(errorMsg, "Lua execution error: " + e.getMessage());
            execResult = ExecutionResult.scriptFailure(moduleName, errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = formatJavaError(script, e);
            addError(errorMsg, "Java exception: " + e.getMessage());
            execResult = ExecutionResult.scriptFailure(moduleName, errorMsg);
        } finally {
            // Always set the module name back to support recursive calls
            Logger.setCurrentModuleName(previousModuleName);
        }
        results.add(execResult);
        return execResult;
    }

    public ExecutionResult executeModule(LuaModuleMetadata metadata, JavaContext context,
            List<LuaModuleMetadata> preModuleScripts, List<LuaModuleMetadata> postModuleScripts,
            ExecutionRequest request) {

        // Execute pre module script(s)
        if (preModuleScripts != null) {
            for (LuaModuleMetadata script : preModuleScripts) {
                try {
                    executeLuaScript(script, context, ModuleRegistry.SCRIPT_TIMING_PRE,
                            ModuleRegistry.SCRIPT_WHEN_MODULE);
                } catch (Exception e) {
                    Logger.error("Error executing pre module script '" + script.getName() + "': "
                            + e.getMessage());
                }
            }
        }

        // Execute the module
        ExecutionResult result = executeLuaModule(request, metadata, context);

        // Execute post module script(s)
        if (postModuleScripts != null) {
            for (LuaModuleMetadata script : postModuleScripts) {
                try {
                    executeLuaScript(script, context, ModuleRegistry.SCRIPT_TIMING_POST,
                            ModuleRegistry.SCRIPT_WHEN_MODULE);
                } catch (Exception e) {
                    Logger.error("Error executing post module script '" + script.getName() + "': "
                            + e.getMessage());
                }
            }
        }

        return result;
    }

    private LuaValue executeWithTraceback(LuaModuleMetadata metadata, LuaTable contextTable,
            LuaTable argsTable) {
        try {
            // get xpcall and debug table from lua sandbox
            LuaValue xpcall = sandbox.getGlobals().get("xpcall");
            LuaValue debug = sandbox.getGlobals().get("debug");

            // check if theyre available so we can use them
            if (!xpcall.isnil() && !debug.isnil() && debug.istable()) {
                LuaValue traceback = debug.get("traceback");
                if (!traceback.isnil()) {
                    // use xpcall wrapper to get better stack traces on errors
                    return executeWithXpcall(metadata, contextTable, argsTable, xpcall, traceback);
                }
            }
        } catch (Exception e) {
            // if setup fails just run without xpcall
        }

        // run without traceback if xpcall isnt available
        return metadata.getExecuteFunction().call(contextTable, argsTable);
    }

    private LuaValue executeWithXpcall(LuaModuleMetadata metadata, LuaTable contextTable,
            LuaTable argsTable, LuaValue xpcall, LuaValue traceback) {
        // make an error handler that captures the full stack trace
        // the 2 skips the error handler and xpcall frames
        LuaValue errorHandler = new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue err) {
                return traceback.call(err, LuaValue.valueOf(2));
            }
        };

        // call the lua function through xpcall so we catch errors
        Varargs result = xpcall.invoke(new LuaValue[] {metadata.getExecuteFunction(), errorHandler,
                contextTable, argsTable});

        // first return is success boolean
        if (!result.arg1().checkboolean()) {
            // failed so second return is the error message with stack trace
            LuaValue errorMsg = result.arg(2);
            throw new LuaError(errorMsg.tojstring());
        }

        // success so second return is the actual result
        return result.arg(2);
    }

    private void setSeedInLua(int seed) {
        try {
            // load the randomizer module
            LuaValue randomizer =
                    sandbox.getGlobals().get("require").call(LuaValue.valueOf("randomizer"));
            // set the seed for random number generation
            randomizer.get("setSeed").call(LuaValue.valueOf(seed));
        } catch (Exception e) {
            // if this fails just warn and keep going
            System.err.println(
                    "[ModuleExecutor] Warning: Could not set seed in Lua: " + e.getMessage());
        }
    }

    public void executeScripts(List<LuaModuleMetadata> scripts, JavaContext context,
            String scriptTiming, String scriptWhen) {
        if (scripts != null) {
            for (LuaModuleMetadata script : scripts) {
                try {
                    executeLuaScript(script, context, scriptTiming, scriptWhen);
                } catch (Exception e) {
                    Logger.error(
                            "Error executing script '" + script.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    // Execute multiple modules with pre/post module scripts for each
    public List<ExecutionResult> executeModules(List<ExecutionRequest> requests,
            ModuleRegistry moduleRegistry, JavaContext context,
            List<LuaModuleMetadata> preModuleScripts, List<LuaModuleMetadata> postModuleScripts) {
        List<ExecutionResult> execResults = new ArrayList<>();

        for (ExecutionRequest request : requests) {
            // Look up the module metadata from the registry
            LuaModuleMetadata module = moduleRegistry.getModule(request.getModuleName());
            if (module == null) {
                String errorMsg = "Module not found: " + request.getModuleName();
                addError(errorMsg, errorMsg);
                ExecutionResult errorResult = ExecutionResult.failure(request, errorMsg);
                execResults.add(errorResult);
                continue;
            }

            // Execute the module with the request (seed is already set in the request)
            ExecutionResult result =
                    executeModule(module, context, preModuleScripts, postModuleScripts, request);
            execResults.add(result);
        }

        return execResults;
    }

    private Map<String, Object> validateArguments(LuaModuleMetadata metadata,
            Map<String, Object> arguments, redactedrice.randomizer.context.JavaContext context) {
        Map<String, Object> validated = new HashMap<>();

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        // need enum context for validating enum arguments
        redactedrice.randomizer.context.EnumContext enumContext =
                context != null ? context.getEnumContext() : null;

        // go through each argument the module expects
        for (ArgumentDefinition argDef : metadata.getArguments()) {
            String argName = argDef.getName();
            Object value = arguments.get(argName);

            // make sure required args are present
            if (value == null && argDef.getDefaultValue() == null) {
                throw new IllegalArgumentException("Missing required argument '" + argName
                        + "' for module '" + metadata.getName() + "'");
            }

            // convert and validate the value
            try {
                Object convertedValue = argDef.convertAndValidate(value, enumContext);
                validated.put(argName, convertedValue);
            } catch (IllegalArgumentException e) {
                // add module and arg name to error message
                String errorMessage = e.getMessage();
                throw new IllegalArgumentException(
                        String.format("Error validating argument '%s' for module '%s': %s", argName,
                                metadata.getName(),
                                errorMessage != null ? errorMessage : "Unknown error"),
                        e);
            }
        }

        return validated;
    }

    private LuaTable convertArgumentsToLuaTable(LuaModuleMetadata metadata,
            Map<String, Object> arguments) {
        LuaTable table = new LuaTable();

        if (arguments != null) {
            // build a map of argument name to type definition for quick lookup
            // this is needed to handle GROUP types specially
            Map<String, redactedrice.randomizer.metadata.TypeDefinition> argTypes = new HashMap<>();
            for (ArgumentDefinition argDef : metadata.getArguments()) {
                argTypes.put(argDef.getName(), argDef.getTypeDefinition());
            }

            // convert each argument to Lua format
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String argName = entry.getKey();
                Object value = entry.getValue();

                // check if this is a GROUP type argument
                // GROUP types need special handling because they need to be wrapped
                redactedrice.randomizer.metadata.TypeDefinition argType = argTypes.get(argName);
                if (argType != null && argType
                        .getBaseType() == redactedrice.randomizer.metadata.TypeDefinition.BaseType.GROUP) {
                    // for group types convert the map to a lua table then wrap it with randomizer
                    // group
                    try {
                        LuaValue mapTable = javaToLuaValue(value);

                        // get the randomizer module and group function
                        LuaValue randomizerModule = sandbox.getGlobals().get("require")
                                .call(LuaValue.valueOf("randomizer"));
                        LuaValue groupFunction = randomizerModule.get("group");

                        if (groupFunction.isnil()) {
                            throw new IllegalStateException(
                                    "randomizer.group function not found. Make sure randomizer module is properly loaded.");
                        }

                        // call randomizer group on the table and set the result
                        LuaValue groupObject = groupFunction.call(mapTable);
                        table.set(argName, groupObject);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to convert argument '" + argName
                                + "' to Group: " + e.getMessage(), e);
                    }
                } else {
                    // regular conversion for non group types
                    LuaValue luaValue = javaToLuaValue(value);
                    table.set(argName, luaValue);
                }
            }
        }

        return table;
    }

    // convert java object to lua value handling collections
    private LuaValue javaToLuaValue(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        } else if (value instanceof List) {
            // convert list to lua table with 1 based indexing
            List<?> list = (List<?>) value;
            LuaTable luaTable = new LuaTable();
            for (int i = 0; i < list.size(); i++) {
                // recursively convert list elements
                luaTable.set(i + 1, javaToLuaValue(list.get(i)));
            }
            return luaTable;
        } else if (value instanceof Map) {
            // convert Map to Lua table
            Map<?, ?> map = (Map<?, ?>) value;
            LuaTable luaTable = new LuaTable();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                // recursively convert map keys and values
                LuaValue key = javaToLuaValue(entry.getKey());
                LuaValue val = javaToLuaValue(entry.getValue());
                luaTable.set(key, val);
            }
            return luaTable;
        } else {
            // use standard coercion for primitives and strings
            return CoerceJavaToLua.coerce(value);
        }
    }

    private void addError(String error, String loggerErrorMsg) {
        errors.add(error);
        Logger.error(loggerErrorMsg);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<ExecutionResult> getResults() {
        return new ArrayList<>(results);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void clearResults() {
        errors.clear();
        results.clear();
    }

    private String formatLuaError(LuaModuleMetadata metadata, LuaError e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lua error in module '").append(metadata.getName()).append("'");

        // add the file name if available
        String filePath = metadata.getFilePath();
        if (filePath != null) {
            sb.append(" (").append(new java.io.File(filePath).getName()).append(")");
        }

        String errorMsg = e.getMessage();

        // check if error message already has a stack traceback from xpcall
        if (errorMsg != null && errorMsg.contains("stack traceback:")) {
            // error already has lua stack trace so just indent it
            sb.append(":\n  ");
            String[] lines = errorMsg.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sb.append(line).append("\n  ");
                }
            }
            // remove trailing " " added by the loop
            if (sb.length() >= 3) {
                sb.setLength(sb.length() - 3);
            }
        } else {
            // no stack trace so show basic error with java stack
            sb.append(": ").append(errorMsg);
            sb.append("\n\n  Java call stack:");

            // include full Java stack trace for debugging
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            // indent each line and filter out "Unknown Source" to make it cleaner
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty() && !line.contains("Unknown Source")) {
                    sb.append("\n    ").append(line.trim());
                }
            }
        }

        return sb.toString();
    }

    private void logExecutionInfo(String moduleName, int seed, Map<String, Object> arguments,
            String scriptTiming, String scriptWhen) {
        // Build script type information
        StringBuilder scriptInfo = new StringBuilder();
        if (scriptTiming != null && scriptWhen != null) {
            // It's a script (pre/post)
            scriptInfo.append(" [Script]");
            if (ModuleRegistry.SCRIPT_WHEN_MODULE.equals(scriptWhen)) {
                scriptInfo.append("[per module]");
            } else if (ModuleRegistry.SCRIPT_WHEN_RANDOMIZE.equals(scriptWhen)) {
                scriptInfo.append("[per randomize]");
            }
            scriptInfo.append("[").append(scriptTiming).append("]");
        } else {
            // It's a regular module
            scriptInfo.append(" [Module]");
        }

        // if no arguments just log seed
        if (arguments == null || arguments.isEmpty()) {
            Logger.info("Starting execution of '" + moduleName + "'" + scriptInfo.toString()
                    + " with seed: " + seed);
        } else {
            // format arguements for logging with nice formatting
            StringBuilder argsStr = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                if (!first) {
                    argsStr.append(", ");
                }
                argsStr.append(entry.getKey()).append("=");
                Object value = entry.getValue();
                // format collections specially so theyre readable
                if (value instanceof Collection) {
                    argsStr.append("[");
                    Collection<?> coll = (Collection<?>) value;
                    boolean firstItem = true;
                    for (Object item : coll) {
                        if (!firstItem) {
                            argsStr.append(", ");
                        }
                        argsStr.append(item);
                        firstItem = false;
                    }
                    argsStr.append("]");
                } else if (value instanceof Map) {
                    // format maps as key value pairs
                    argsStr.append("{");
                    Map<?, ?> map = (Map<?, ?>) value;
                    boolean firstItem = true;
                    for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
                        if (!firstItem) {
                            argsStr.append(", ");
                        }
                        argsStr.append(mapEntry.getKey()).append("=").append(mapEntry.getValue());
                        firstItem = false;
                    }
                    argsStr.append("}");
                } else {
                    // everything else just use toString
                    argsStr.append(value);
                }
                first = false;
            }
            Logger.info("Starting execution of '" + moduleName + "'" + scriptInfo.toString()
                    + " with seed: " + seed + " and args: " + argsStr.toString());
        }
    }

    private String formatJavaError(LuaModuleMetadata metadata, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error executing module '").append(metadata.getName()).append("'");

        // add the file name if available
        String filePath = metadata.getFilePath();
        if (filePath != null) {
            sb.append(" (").append(new java.io.File(filePath).getName()).append(")");
        }

        sb.append(": ").append(e.getMessage());

        // add cause info if available for debugging
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            sb.append("\n  Caused by: ").append(cause.getMessage());
        }

        return sb.toString();
    }

}
