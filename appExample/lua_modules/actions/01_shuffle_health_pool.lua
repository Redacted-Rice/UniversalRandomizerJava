-- Example module 1: Shuffle HP values using consumable pool
-- Takes HP values from original entities and randomly assigns them to modified entities

local randomizer = require("randomizer")

return {
	name = "01_shuffle_health_pool",
	description = "Pulls HP values from original entities and randomly assigns them using an exhausting pool",
	group = "players",
	modifies = { "health" },
	author = "Redacted Rice",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local entitiesOriginal = context.entitiesOriginal
		local entitiesModified = context.entitiesModified

		-- Create consumable pool by extracting health values
		local healthPool = randomizer.listFromField(entitiesOriginal, "getHealth")

		-- Randomize modified entities' health using the consumable pool
		local setter = function(entity, value)
			entity:setHealth(value)
		end
		randomizer.randomize(entitiesModified, healthPool, setter, {
			consumable = true,
		})
	end,
}
