-- Example module 6: Two-phase starting item assignment
-- Phase 1: Assign weighted random rarity to each entity
-- Phase 2: Assign random item from that rarity group

local randomizer = require("randomizer")

return {
	name = "06_assign_starting_items",
	description = "Two-phase assignment: weighted rarity, then grouped item selection",
	groups = { "players" },
	modifies = { "startingItem", "startingItemRarity" },
	author = "Redacted Rice",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

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
		local itemsModified = context.itemsModified
		local ItemRarity = context.ItemRarity

		-- Phase 1: Assign starting item rarity to each entity
		-- Assign rarities using the randomizer
		randomizer.randomize(entitiesModified, args.weightedRarityPool, function(entity, rarity)
			-- Create a new field in the entity to store rarity to aid in randomization
			entity.startingItemRarity = rarity
		end)

		-- Create grouped pool of item names by rarity
		local itemNamesByRarity = randomizer.groupFromField(itemsModified, "rarity", "name")

		-- Phase 2: Assign item based on the assigned rarity
		local setter = function(entity, itemName)
			entity:setStartingItem(itemName)
		end
		randomizer.randomize(entitiesModified, itemNamesByRarity, "startingItemRarity", setter, {
			consumable = true,
		})
	end,
}
