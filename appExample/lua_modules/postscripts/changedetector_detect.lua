-- Checks for changes and logs if there are any
return {
	 id = "changedetector_detect",
	name = "changedetector_detect",
	description = "Detect and log changes after each module",
	when = "module",
	author = "Redacted Rice",
	version = "1.0.0",

	requires = {
		ExampleApp = "0.9.0",
		changedetector_setup = "0.1.0",
		changedetector_snapshot = "0.58.123",
	},

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		local changes = changedetector.detectChanges()
		if changedetector.hasChanges(changes) then
			local formatOptions = {
				leadingNewline = true,
			}
			if context.executionModule then
				formatOptions.moduleName = context.executionModule
			end

			logger.info(changedetector.formatChangesTable(changes, formatOptions))
		end
	end,
}
