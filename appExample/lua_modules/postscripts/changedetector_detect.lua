-- POST-MODULE script: Detect and log changes after each module runs
-- Timing: Runs after each randomization module executes
-- Purpose: Compares current state to snapshot and logs changes in compact format

return {
	name = "changedetector_detect",
	description = "Detect and log changes after each module",
	when = "post-module",

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		-- Detect changes since last snapshot
		local changes = changedetector.detectChanges()

		-- Build complete change log string if there are any changes
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

			-- Log everything as a single string (logger already outputs to console if configured)
			local logString = table.concat(lines, "\n")
			logger.info(logString)
		end
	end,
}

