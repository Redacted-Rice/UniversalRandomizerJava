-- Checks for changes and logs if there are any
return {
	name = "changedetector_detect",
	description = "Detect and log changes after each module",
	when = "module",
	author = "Redacted Rice",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		local changes = changedetector.detectChanges()
		if changedetector.hasChanges(changes) then
			-- Layout comes from monitor setup; this only renders the ASCII table
			logger.info(changedetector.formatChangesTable(changes))
		end
	end,
}
