-- Types come from the EE_EntityTypes enum.
local entities = {
	{ name = "Hero", type = "WARRIOR" },
	{ name = "Mage", type = "MAGE" },
}

return {
	{
		name = "assigns_random_types",
		module = "02_randomize_entity_types",
		seed = 42,
		entities = entities,
		expect = {
			{ name = "Hero", type = "RANGER" },
			{ name = "Mage", type = "RANGER" },
		},
	},
}
