-- minimal_groups.lua
-- Minimal module with just a groups tag to verify that still loads

local randomizer = require("randomizer")

return {
	id = "minimal_groups_test",
	name = "Minimal Groups Test",
	description = "Tests that a module with only groups metadata loads",
	groups = { "test" },
	defaultSeedOffset = 88,
	author = "Test Author",
	version = "0.1",

	execute = function(context, args)
		return true
	end,
}
