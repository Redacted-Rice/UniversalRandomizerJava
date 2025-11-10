-- PRE-RANDOMIZE script: Configure change detection before any modules run
-- Timing: Runs once before any randomization modules execute
-- Purpose: Sets up monitoring entries for objects that will be modified
-- Note: Java ModuleExecutor automatically calls takeSnapshots() before each module
--       and the detect script after each module

return {
	name = "changedetector_setup",
	description = "Setup change detection with multiple monitoring entries",
	when = "pre-randomize",

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		-- Get configuration from context
		local changeLogFile = context.config and context.config.changeLogFile
		local outputToConsole = context.config and context.config.outputToConsole

		-- Configure base settings
		changedetector.configure(changeLogFile, outputToConsole)

		-- Skip monitoring setup if change detection is not active
		if not changedetector.isActive() then
			return
		end

		-- Define entity fields to monitor
		local entityFields = {"name", "health", "damage", "speed", "defense", "type", "startingItem"}

		-- Define item fields to monitor
		local itemFields = {"name", "rarity", "attackBonus", "defenseBonus", "healthBonus", "speedBonus"}

		-- Monitor the entities with explicit field list
		changedetector.monitor("Modified Entities", context.entitiesModified, entityFields, function(obj)
			return obj:getName()
		end)

		-- Monitor the items with explicit field list (items use public field, not getter)
		changedetector.monitor("Modified Items", context.itemsModified, itemFields, function(obj)
			return obj.name
		end)

		-- Log what entries were configured
		local entries = changedetector.getMonitoredEntryNames()
		if #entries > 0 then
			logger.info("Change detection configured with " .. #entries .. " monitoring entry(ies)")
			for _, entryName in ipairs(entries) do
				logger.info("  - Monitoring: " .. entryName)
			end
			if changeLogFile then
				logger.info("  - File: " .. changeLogFile)
			end
			if outputToConsole then
				logger.info("  - Also outputting to console")
			end
		end

		logger.info("Prescript changedetector_setup completed")
	end,
}
