-- Configure the change detector for our example
return {
	name = "changedetector_setup",
	description = "Setup change detection on objects being randomized",
	when = "randomize",

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		-- Get active flag from config
		local isActive = context.config and context.config.changeDetectionActive or false
		changedetector.configure(isActive)

        -- Setup monitoring for entities and items
        -- TODO It would be nice if we could autodetect these in the wrapper
		local entityFields = {"name", "health", "damage", "speed", "defense", "type", "startingItem"}
		local itemFields = {"name", "rarity", "attackBonus", "defenseBonus", "healthBonus", "speedBonus"}

		changedetector.monitor("Entities", context.entitiesModified, entityFields, function(obj)
			return obj:getName()
		end)
		changedetector.monitor("Items", context.itemsModified, itemFields, function(obj)
			return obj.name
		end)

		-- Log just so its clear what we are tracking
		local entries = changedetector.getMonitoredEntryNames()
		if #entries > 0 then
			logger.info("Change detection configured with " .. #entries .. " monitoring entries")
			for _, entryName in ipairs(entries) do
				logger.info("  - Monitoring: " .. entryName)
			end
		end

		logger.info("Prescript changedetector_setup completed")
	end,
}
