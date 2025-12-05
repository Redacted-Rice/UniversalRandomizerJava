-- Example module 4: Randomize defense and attack as tuples based on entity type
-- Groups defense/attack pairs by type, then assigns them together to maintain balance

local randomizer = require("randomizer")

return {
	name = "04_grouped_stats_by_type",
	description = "Randomizes defense and attack tuple values based on entity type using grouped pools",
	groups = { "players" },
	modifies = { "defense", "damage" },
	author = "Redacted Rice",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local entitiesOriginal = context.entitiesOriginal
		local entitiesModified = context.entitiesModified

		-- Create grouped pool of stat tuples
		local statTuplesPool = randomizer.groupFromField(entitiesOriginal, "getType", function(entity)
			return { defense = entity:getDefense(), damage = entity:getDamage() }
		end)

		-- Setter function that applies both defense and damage from the tuple
		local setter = function(entity, tuple)
			entity:setDefense(tuple.defense)
			entity:setDamage(tuple.damage)
		end

		-- Randomize both stats together based on type
		randomizer.randomize(entitiesModified, statTuplesPool, "getType", setter)
	end,
}
