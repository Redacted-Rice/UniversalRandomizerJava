package redactedrice.randomizer.lua;

import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.utils.Logger;
import redactedrice.randomizer.utils.ErrorTracker;
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
    ModuleArgumentValidator argumentValidator;
    ModuleArgumentConverter argumentConverter;

    public ModuleExecutor(LuaSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        this.sandbox = sandbox;
        this.results = new ArrayList<>();
        this.argumentValidator = new ModuleArgumentValidator();
        this.argumentConverter = new ModuleArgumentConverter(sandbox);
    }

    private ExecutionResult executeModule(ExecutionRequest request, Module metadata,
            JavaContext context, int baseSeed) {
        if (metadata == null) {
            throw new IllegalArgumentException("Module metadata cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("ExecutionRequest cannot be null");
        }

        boolean usesSeed = metadata.isSeeded();
        int seedUsed = 0;
        int absoluteSeed = 0;
        if (usesSeed) {
            absoluteSeed = request.resolveAbsoluteSeed(baseSeed);
            seedUsed = absoluteSeed;
        }

        String moduleName = metadata.getName();
        String previousModuleName = Logger.getCurrentModuleName();
        ExecutionResult execResult = null;

        Logger.setCurrentModuleName(moduleName);
        try {
            // validate and convert arguements using enum context from javacontext
            Map<String, Object> validatedArgs =
                    argumentValidator.validate(metadata, request.getArguments(), context);

            ExecutionErrorFormatter.logExecutionInfo(moduleName, validatedArgs, null, null,
                    metadata, baseSeed, absoluteSeed, request);

            if (usesSeed) {
                setSeedInLua(absoluteSeed);
            }
            LuaTable argsTable = argumentConverter.toLuaTable(metadata, validatedArgs);

            // Execute and return the results
            LuaValue result = executeWithTraceback(metadata, context.toLuaTable(), argsTable);
            execResult = ExecutionResult.success(request, seedUsed, result);
        } catch (LuaError e) {
            String errorMsg = ExecutionErrorFormatter.formatLuaError(metadata, e);
            ErrorTracker.addError(errorMsg);
            Logger.error(errorMsg);
            execResult = ExecutionResult.failure(request, seedUsed, errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = ExecutionErrorFormatter.formatJavaError(metadata, e);
            ErrorTracker.addError(errorMsg);
            Logger.error(errorMsg);
            execResult = ExecutionResult.failure(request, seedUsed, errorMsg);
        } finally {
            if (execResult != null && execResult.isSuccess()) {
                Logger.info("Finished execution of '" + moduleName + "'");
            }
            // Always set the module name back to support recursive calls
            Logger.setCurrentModuleName(previousModuleName);
        }
        results.add(execResult);
        return execResult;
    }

    // Pre/post scripts intentionally omit arguments and seed handling
    private ExecutionResult executeLuaScript(Module script, JavaContext context,
            String scriptTiming, String scriptWhen, String executionModuleName) {
        if (script == null) {
            throw new IllegalArgumentException("Script metadata cannot be null");
        }
        if (scriptTiming == null || scriptWhen == null) {
            throw new IllegalArgumentException("Script timing and when cannot be null");
        }

        ExecutionRequest request = ExecutionRequest.forScript(script);
        String moduleName = script.getName();
        String previousModuleName = Logger.getCurrentModuleName();
        ExecutionResult execResult = null;

        Logger.setCurrentModuleName(moduleName);
        try {
            if (executionModuleName != null) {
                context.setExecutionModuleName(executionModuleName);
            }

            ExecutionErrorFormatter.logExecutionInfo(moduleName, Map.of(), scriptTiming,
                    scriptWhen, script, request);

            LuaTable argsTable = new LuaTable();
            LuaValue result = executeWithTraceback(script, context.toLuaTable(), argsTable);
            execResult = ExecutionResult.scriptSuccess(request, result);
        } catch (LuaError e) {
            String errorMsg = ExecutionErrorFormatter.formatLuaError(script, e);
            ErrorTracker.addError(errorMsg);
            execResult = ExecutionResult.scriptFailure(request, errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = ExecutionErrorFormatter.formatJavaError(script, e);
            ErrorTracker.addError(errorMsg);
            execResult = ExecutionResult.scriptFailure(request, errorMsg);
        } finally {
            if (executionModuleName != null) {
                context.clearExecutionModuleName();
            }

            // Always set the module name back to support recursive calls
            Logger.setCurrentModuleName(previousModuleName);
        }
        results.add(execResult);
        return execResult;
    }

    public ExecutionResult executeModule(Module metadata, JavaContext context,
            List<Module> preModuleScripts, List<Module> postModuleScripts, ExecutionRequest request,
            int baseSeed) {

        // Execute pre module script(s)
        if (preModuleScripts != null) {
            for (Module script : preModuleScripts) {
                try {
                    executeLuaScript(script, context, ModuleRegistry.SCRIPT_TIMING_PRE,
                            ModuleRegistry.SCRIPT_WHEN_MODULE, null);
                } catch (Exception e) {
                    Logger.error("Error executing pre module script '" + script.getName() + "': "
                            + e.getMessage());
                }
            }
        }

        // Execute the module
        ExecutionResult result = executeModule(request, metadata, context, baseSeed);
        String executedModuleName = metadata.getName();

        // Execute post module script(s)
        if (postModuleScripts != null) {
            for (Module script : postModuleScripts) {
                try {
                    executeLuaScript(script, context, ModuleRegistry.SCRIPT_TIMING_POST,
                            ModuleRegistry.SCRIPT_WHEN_MODULE, executedModuleName);
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
                    executeLuaScript(script, context, scriptTiming, scriptWhen, null);
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
            List<Module> postModuleScripts, int baseSeed) {
        List<ExecutionResult> execResults = new ArrayList<>();

        for (ExecutionRequest request : requests) {
            // Look up the module metadata from the registry
            Module module = moduleRegistry.getModule(request.getModuleName());
            if (module == null) {
                String errorMsg = "Module not found: " + request.getModuleName();
                ErrorTracker.addError(errorMsg);
                int seedUsed = request.usesSeed() ? request.resolveAbsoluteSeed(baseSeed) : 0;
                ExecutionResult errorResult = ExecutionResult.failure(request, seedUsed, errorMsg);
                execResults.add(errorResult);
                continue;
            }

            // Execute the module with the request (seed is resolved from metadata if needed)
            ExecutionResult result = executeModule(module, context, preModuleScripts,
                    postModuleScripts, request, baseSeed);
            execResults.add(result);
        }

        return execResults;
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
