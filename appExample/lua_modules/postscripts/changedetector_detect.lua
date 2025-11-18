-- Checks for changes and logs if there are any
return {
	name = "changedetector_detect",
	description = "Detect and log changes after each module",
	when = "module",
	author = "Example Author",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		-- Get the changes and log if there are any
		local changes = changedetector.detectChanges()
		if changedetector.hasChanges(changes) then
			local lines = {}
			for entryName, entryChanges in pairs(changes) do
				table.insert(lines, "=== Changes detected for " .. entryName .. " ===")

				for identifier, fieldChanges in pairs(entryChanges) do
					table.insert(lines, identifier)

					for fieldName, change in pairs(fieldChanges) do
						table.insert(lines, string.format("  %s: %s -> %s", fieldName, change.old, change.new))
					end
					table.insert(lines, "")
				end
				table.insert(lines, "")
			end

			-- Log everything as a single string so its easier to read in the log
			local logString = table.concat(lines, "\n")
			logger.info(logString)
		end
	end,
}

