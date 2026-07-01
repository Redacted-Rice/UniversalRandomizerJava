package redactedrice.randomizer.lua;

import org.luaj.vm2.LuaError;
import redactedrice.randomizer.utils.Logger;

import java.util.Collection;
import java.util.Map;

public class ExecutionErrorFormatter {

    public static String formatLuaError(Module metadata, LuaError e) {
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

    public static String formatJavaError(Module metadata, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Java error executing module '").append(metadata.getName()).append("'");

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

    // Pre/post scripts omit seed info so pass only the request overload for scripts
    public static void logExecutionInfo(String moduleName, Map<String, Object> arguments,
            String scriptTiming, String scriptWhen, Module module, ExecutionRequest request) {
        logExecutionInfo(moduleName, arguments, scriptTiming, scriptWhen, module, 0, 0, request);
    }

    public static void logExecutionInfo(String moduleName, Map<String, Object> arguments,
            String scriptTiming, String scriptWhen, Module module, int baseSeed, int absoluteSeed,
            ExecutionRequest request) {
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

        String seedInfo = formatSeedLog(module, baseSeed, absoluteSeed, request);

        // if no arguments just log seed
        if (arguments == null || arguments.isEmpty()) {
            Logger.info("Starting execution of '" + moduleName + "'" + scriptInfo + seedInfo);
        } else {
            // format arguments for logging with nice formatting
            String argsStr = formatArguments(arguments);
            Logger.info("Starting execution of '" + moduleName + "'" + scriptInfo + seedInfo
                    + " and args: " + argsStr);
        }
    }

    private static String formatSeedLog(Module module, int baseSeed, int absoluteSeed,
            ExecutionRequest request) {
        // Pre/post scripts intentionally omit arguments and seed handling
        if (request.isScript()) {
            return "";
        }
        int seedOffset = request.getSeedOffset();
        if (request.hasExplicitSeedOffset()) {
            return " with seed: " + absoluteSeed + " (base: " + baseSeed + ", explicit seedOffset: "
                    + seedOffset + ")";
        }
        if (module.isSeedOffsetFromMetadata()) {
            return " with seed: " + absoluteSeed + " (base: " + baseSeed + ", defaultSeedOffset: "
                    + seedOffset + ")";
        }
        return " with seed: " + absoluteSeed + " (base: " + baseSeed + ", nameHashOffset: "
                + seedOffset + ")";
    }

    private static String formatArguments(Map<String, Object> arguments) {
        StringBuilder argsStr = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (!first) {
                argsStr.append(", ");
            }
            argsStr.append(entry.getKey()).append("=");
            Object value = entry.getValue();

            // format collections specially so they're readable
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
        return argsStr.toString();
    }
}
