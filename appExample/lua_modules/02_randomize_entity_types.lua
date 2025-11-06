-- Example module 2: Randomize entity types
-- Assigns random entity types from the registered EntityType enum

local randomizer = require("randomizer")

return {
	name = "02_randomize_entity_types",
	description = "Randomizes entity types using the registered enum",
	group = "players",
	modifies = { "type" },

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
