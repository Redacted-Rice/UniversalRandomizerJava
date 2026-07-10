-- Checks for changes and logs if there are any
return {
	 id = "changedetector_detect",
	name = "changedetector_detect",
	description = "Detect and log changes after each module",
	when = "module",
	author = "Redacted Rice",
	version = "1.0.0",

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
