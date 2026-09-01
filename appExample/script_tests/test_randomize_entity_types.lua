-- Types come from the EE_EntityTypes enum.
-- tags/perkRanks use standard getX/setX names. ranks uses custom method names.
local fields = require("support.fields")

local entities = {
	{
		name = "Hero",
		type = "WARRIOR",
		tags = fields.tags({ { label = "veteran" }, { label = "scout" } }),
		ranks = fields.ranks({ { label = "captain" } }),
		perkRanks = fields.perkRanks({ melee = 2, range = 1 }),
	},
	{
		name = "Mage",
		type = "MAGE",
		tags = fields.tags({ { label = "arcane" } }),
		ranks = fields.ranks({ { label = "adept" } }),
		perkRanks = fields.perkRanks({ arcane = 3 }),
	},
}

return {
	{
		name = "assigns_random_types",
		module = "02_randomize_entity_types",
		seed = 42,
		entities = entities,
		expect = {
			{
				name = "Hero",
				type = "RANGER",
				tags = fields.tags({ { label = "veteran" }, { label = "scout" } }),
				ranks = fields.ranks({ { label = "captain" } }),
				perkRanks = fields.perkRanks({ melee = 2, range = 1 }),
			},
			{
				name = "Mage",
				type = "RANGER",
				tags = fields.tags({ { label = "arcane" } }),
				ranks = fields.ranks({ { label = "adept" } }),
				perkRanks = fields.perkRanks({ arcane = 3 }),
			},
		},
	},
}
