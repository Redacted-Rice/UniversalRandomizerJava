-- Small deck so the shuffle is easy to read. Has multiple test cases in one file
local entities = {
	{ name = "Hero", health = 50 },
	{ name = "Mage", health = 80 },
	{ name = "Rogue", health = 120 },
}

return {
	{
		name = "seed_42",
		module = "01_shuffle_health_pool",
		seed = 42,
		entities = entities,
		-- 50/80/120 landed as 120/50/80 with this seed
		expect = {
			{ name = "Hero", health = 120 },
			{ name = "Mage", health = 50 },
			{ name = "Rogue", health = 80 },
		},
	},
	{
		name = "seed_7",
		module = "01_shuffle_health_pool",
		seed = 7,
		entities = entities,
		-- Same deck, different order
		expect = {
			{ name = "Hero", health = 50 },
			{ name = "Mage", health = 120 },
			{ name = "Rogue", health = 80 },
		},
	},
}
