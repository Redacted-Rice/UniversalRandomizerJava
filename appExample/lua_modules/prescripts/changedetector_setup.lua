-- Configure the change detector for our example
return {
	id = "changedetector_setup",
	name = "changedetector_setup",
	description = "Setup change detection on objects being randomized",
	when = "randomize",
	author = "Redacted Rice",
	version = "1.0.0",

	requires = {
		ExampleApp = "1.0.0",
	},

	execute = function(context)
		local changedetector = require("randomizer").changedetector

		-- Respect the GUI/config toggle for whether change detection runs at all
		local isActive = context.config and context.config.changeDetectionActive or false
		changedetector.configure(isActive)

		-- Table layout is configured here so formatting stays simple in detectChanges()
		changedetector.monitor("Entities", context.entitiesModified, {
			title = "Entities",
			primaryKey = {
				header = "Name",
				getter = function(obj)
					return obj:getName()
				end,
			},
			fields = {
				{ field = "health", header = "Health", align = "right", getter = function(obj) return obj:getHealth() end },
				{ field = "damage", header = "Damage", align = "right", getter = function(obj) return obj:getDamage() end },
				{ field = "speed", header = "Speed", align = "right", getter = function(obj) return obj:getSpeed() end },
				{ field = "defense", header = "Defense", align = "right", getter = function(obj) return obj:getDefense() end },
				{ field = "type", header = "Type", getter = function(obj) return obj:getType() end },
				{ field = "startingItemRarity", header = "Starting Rarity" },
				{ field = "startingItem", header = "Starting Item", getter = function(obj) return obj:getStartingItem() end },
			},
		})

		-- Items use plain table fields instead of Java getters
		changedetector.monitor("Items", context.itemsModified, {
			title = "Items",
			primaryKey = { field = "name", header = "Name" },
			fields = {
				{ field = "rarity", header = "Rarity" },
				{ field = "attackBonus", header = "Attack Bonus", align = "right" },
				{ field = "defenseBonus", header = "Defense Bonus", align = "right" },
				{ field = "healthBonus", header = "Health Bonus", align = "right" },
				{ field = "speedBonus", header = "Speed Bonus", align = "right" },
			},
		})

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
