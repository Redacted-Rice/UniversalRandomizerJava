-- Example module 7: Pick a starting item name from the rarity assigned in phase 1.
-- Depends on 06_assign_starting_item_rarity having set entity.startingItemRarity.

local randomizer = require("randomizer")

return {
	id = "07_assign_starting_item_from_rarity",
	name = "Assign Starting Item From Rarity",
	description = "Assigns a starting item based on the rarity stored on each entity",
	groups = { "players", "startingItem" },
	author = "Redacted Rice",
	version = "1.0.0",

	requires = {
		ExampleApp = "0.9.0",
		["06_assign_starting_item_rarity"] = "0.1.0",
	},

	defaultSeedOffset = 107,

	execute = function(context)
		local entitiesModified = context.entitiesModified
		local itemsModified = context.itemsModified

		local itemNamesByRarity = randomizer.groupFromField(itemsModified, "rarity", "name")

		randomizer.randomize(entitiesModified, itemNamesByRarity, "startingItemRarity",
			function(entity, itemName)
				entity:setStartingItem(itemName)
			end, {
				consumable = true,
				regenerate = true,
			})
	end,
}
