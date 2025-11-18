-- Example module 5: Shuffle all item stats within same rarity using grouped randomization
-- Creates stat tuples grouped by rarity, then shuffles entire stat sets together

local randomizer = require("randomizer")

return {
	name = "05_shuffle_items_by_rarity",
	description = "Shuffles all item stat tuples within the same rarity using grouped consumable pools",
	group = "utils",
	modifies = { "attackBonus", "defenseBonus", "healthBonus", "speedBonus" },
	author = "Example Author",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local itemsOriginal = context.itemsOriginal
		local itemsModified = context.itemsModified

		-- Create grouped pool of stat tuples by rarity from original items
		local statTuplesPool = randomizer.groupFromField(itemsOriginal, "rarity", function(item)
			return {
				attack = item.attackBonus,
				defense = item.defenseBonus,
				health = item.healthBonus,
				speed = item.speedBonus,
			}
		end)

		-- Setter function that applies all stats from the tuple
		local setter = function(item, tuple)
			item.attackBonus = tuple.attack
			item.defenseBonus = tuple.defense
			item.healthBonus = tuple.health
			item.speedBonus = tuple.speed
		end

		-- Shuffle all stats together within rarity groups
		randomizer.randomize(itemsModified, statTuplesPool, "rarity", setter, {
			consumable = true,
		})
	end,
}
