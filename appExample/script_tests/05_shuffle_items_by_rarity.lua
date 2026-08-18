-- Two commons with different stats so a same rarity shuffle is visible.
local items = {
	{ name = "Stick", rarity = "COMMON", attackBonus = 1, defenseBonus = 0, healthBonus = 0, speedBonus = 0 },
	{ name = "Cloth", rarity = "COMMON", attackBonus = 0, defenseBonus = 4, healthBonus = 2, speedBonus = 1 },
	{ name = "Excalibur", rarity = "LEGENDARY", attackBonus = 25, defenseBonus = 8, healthBonus = 50, speedBonus = 5 },
}

return {
	{
		name = "shuffles_stats_within_rarity",
		module = "05_shuffle_items_by_rarity",
		seed = 42,
		items = items,
		-- Commons swapped stat tuples. Legendary stayed put because it is the only one in its bucket.
		expect = {
			{ name = "Stick", attackBonus = 0, defenseBonus = 4, healthBonus = 2, speedBonus = 1 },
			{ name = "Cloth", attackBonus = 1, defenseBonus = 0, healthBonus = 0, speedBonus = 0 },
			{ name = "Excalibur", attackBonus = 25, defenseBonus = 8, healthBonus = 50, speedBonus = 5 },
		},
	},
}
