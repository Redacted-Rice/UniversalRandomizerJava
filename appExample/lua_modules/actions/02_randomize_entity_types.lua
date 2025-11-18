-- Example module 2: Randomize entity types
-- Assigns random entity types from the registered EntityType enum

local randomizer = require("randomizer")

return {
	name = "02_randomize_entity_types",
	description = "Randomizes entity types using the registered enum",
	group = "players",
	modifies = { "type" },
	author = "Example Author",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local entitiesModified = context.entitiesModified
		local EE_EntityTypes = context.EE_EntityTypes

		-- Randomize types using non-consumable pool
		local setter = function(entity, enumValue)
			entity:setType(enumValue)
		end
		randomizer.randomize(entitiesModified, EE_EntityTypes, setter)

	end,
}
