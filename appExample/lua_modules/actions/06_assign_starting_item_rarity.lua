-- Example module 6: Assign weighted starting-item rarity to each entity.
-- Stores rarity on the entity wrapper as a dynamic field for downstream modules.

local randomizer = require("randomizer")

return {
	id = "06_assign_starting_item_rarity",
	name = "Assign Starting Item Rarity",
	description = "Assigns a weighted random starting-item rarity to each entity",
	groups = { "players" },
	modifies = { "startingItemRarity" },
	author = "Redacted Rice",
	version = "1.0.0",

	requires = {
		ExampleApp = "1.0.0",
	},

	defaultSeedOffset = 106,

	arguments = {
		{
			name = "weightedRarityPool",
			definition = {
				type = "list",
				elementDefinition = {
					type = "enum",
					constraint = "ItemRarity",
				},
			},
		},
	},

	execute = function(context, args)
		local entitiesModified = context.entitiesModified

		randomizer.randomize(entitiesModified, args.weightedRarityPool, function(entity, rarity)
			entity.startingItemRarity = rarity
		end)
	end,
}
