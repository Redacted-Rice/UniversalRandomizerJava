package redactedrice.randomizer.lua;

import redactedrice.randomizer.context.EnumRegistry;
import redactedrice.randomizer.context.JavaContext;
import redactedrice.randomizer.lua.arguments.ArgumentDefinition;

import java.util.HashMap;
import java.util.Map;

// Validates module arguments against their definitions
public class ModuleArgumentValidator {

    // Validate and convert arguments for a module
    public Map<String, Object> validate(Module module, Map<String, Object> arguments,
            JavaContext context) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }

        Map<String, Object> validated = new HashMap<>();

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        // need enum registry for validating enum arguments
        EnumRegistry enumRegistry = context != null ? context.getEnumRegistry() : null;

        // go through each argument the module expects
        for (ArgumentDefinition argDef : module.getArguments()) {
            String argName = argDef.getName();
            Object value = arguments.get(argName);

            // make sure required args are present
            if (value == null && argDef.getDefaultValue() == null) {
                throw new IllegalArgumentException("Missing required argument '" + argName
                        + "' for module '" + module.getName() + "'");
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
                                module.getName(),
                                errorMessage != null ? errorMessage : "Unknown error"),
                        e);
            }
        }

        return validated;
    }
}
