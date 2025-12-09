package redactedrice.randomizer.lua;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.LuaJavaConverter;
import redactedrice.randomizer.utils.Logger;
import redactedrice.randomizer.utils.ErrorTracker;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;
import redactedrice.randomizer.lua.arguments.ArgumentType;
import redactedrice.randomizer.lua.arguments.TypeDefinition;
import redactedrice.randomizer.lua.sandbox.LuaSandbox;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;

import java.util.*;

// runs lua modules with the given context and arguments
public class ModuleExecutor {
    LuaSandbox sandbox;
    List<ExecutionResult> results;

    public ModuleExecutor(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
        this.results = new ArrayList<>();
    }

    private ExecutionResult executeModule(ExecutionRequest request, Module metadata,
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

            ExecutionErrorFormatter.logExecutionInfo(moduleName, request.getSeed(), validatedArgs,
                    null, null);

            // set seed and get the args
            setSeedInLua(request.getSeed());
            LuaTable argsTable = convertArgumentsToLuaTable(metadata, validatedArgs);

            // Execute and return the results
            LuaValue result = executeWithTraceback(metadata, context.toLuaTable(), argsTable);
            execResult = ExecutionResult.success(request, result);
        } catch (LuaError e) {
            String errorMsg = ExecutionErrorFormatter.formatLuaError(metadata, e);
            ErrorTracker.addError(errorMsg);
            execResult = ExecutionResult.failure(request, errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = ExecutionErrorFormatter.formatJavaError(metadata, e);
            ErrorTracker.addError(errorMsg);
            execResult = ExecutionResult.failure(request, errorMsg);
        } finally {
            // Always set the module name back to support recursive calls
            Logger.setCurrentModuleName(previousModuleName);
        }
        results.add(execResult);
        return execResult;
    }

    private ExecutionResult executeLuaScript(Module script, JavaContext context,
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
            ExecutionErrorFormatter.logExecutionInfo(moduleName, 0, null, scriptTiming, scriptWhen);
            LuaValue result = executeWithTraceback(script, context.toLuaTable(), new LuaTable());
            execResult = ExecutionResult.scriptSuccess(moduleName, result);
        } catch (LuaError e) {
            String errorMsg = ExecutionErrorFormatter.formatLuaError(script, e);
            ErrorTracker.addError(errorMsg);
            execResult = ExecutionResult.scriptFailure(moduleName, errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = ExecutionErrorFormatter.formatJavaError(script, e);
            ErrorTracker.addError(errorMsg);
            execResult = ExecutionResult.scriptFailure(moduleName, errorMsg);
        } finally {
            // Always set the module name back to support recursive calls
            Logger.setCurrentModuleName(previousModuleName);
        }
        results.add(execResult);
        return execResult;
    }

    public ExecutionResult executeModule(Module metadata, JavaContext context,
            List<Module> preModuleScripts, List<Module> postModuleScripts,
            ExecutionRequest request) {

        // Execute pre module script(s)
        if (preModuleScripts != null) {
            for (Module script : preModuleScripts) {
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
        ExecutionResult result = executeModule(request, metadata, context);

        // Execute post module script(s)
        if (postModuleScripts != null) {
            for (Module script : postModuleScripts) {
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

    private LuaValue executeWithTraceback(Module metadata, LuaTable contextTable,
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

    private LuaValue executeWithXpcall(Module metadata, LuaTable contextTable, LuaTable argsTable,
            LuaValue xpcall, LuaValue traceback) {
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

    public void executeScripts(List<Module> scripts, JavaContext context, String scriptTiming,
            String scriptWhen) {
        if (scripts != null) {
            for (Module script : scripts) {
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
            ModuleRegistry moduleRegistry, JavaContext context, List<Module> preModuleScripts,
            List<Module> postModuleScripts) {
        List<ExecutionResult> execResults = new ArrayList<>();

        for (ExecutionRequest request : requests) {
            // Look up the module metadata from the registry
            Module module = moduleRegistry.getModule(request.getModuleName());
            if (module == null) {
                String errorMsg = "Module not found: " + request.getModuleName();
                ErrorTracker.addError(errorMsg);
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

    private Map<String, Object> validateArguments(Module metadata, Map<String, Object> arguments,
            redactedrice.randomizer.context.JavaContext context) {
        Map<String, Object> validated = new HashMap<>();

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        // need enum registry for validating enum arguments
        redactedrice.randomizer.context.EnumRegistry enumRegistry =
                context != null ? context.getEnumRegistry() : null;

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
                Object convertedValue = argDef.convertAndValidate(value, enumRegistry);
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

    private LuaTable convertArgumentsToLuaTable(Module metadata, Map<String, Object> arguments) {
        LuaTable table = new LuaTable();

        if (arguments != null) {
            // build a map of argument name to type definition for quick lookup
            // this is needed to handle GROUP types specially
            Map<String, TypeDefinition> argTypes = new HashMap<>();
            for (ArgumentDefinition argDef : metadata.getArguments()) {
                argTypes.put(argDef.getName(), argDef.getTypeDefinition());
            }

            // convert each argument to Lua format
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String argName = entry.getKey();
                Object value = entry.getValue();

                // check if this is a GROUP type argument
                // GROUP types need special handling because they need to be wrapped
                TypeDefinition argType = argTypes.get(argName);
                if (argType != null && argType.getBaseType() == ArgumentType.GROUP) {
                    // for group types convert the map to a lua table then wrap it with randomizer
                    // group
                    try {
                        LuaValue mapTable = LuaJavaConverter.javaToLua(value);

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
                    LuaValue luaValue = LuaJavaConverter.javaToLua(value);
                    table.set(argName, luaValue);
                }
            }
        }

        return table;
    }

    public List<ExecutionResult> getResults() {
        return new ArrayList<>(results);
    }

    public void clearResults() {
        // TODO: Handle this better - probably means making non static
        ErrorTracker.clearErrors();
        results.clear();
    }
}
