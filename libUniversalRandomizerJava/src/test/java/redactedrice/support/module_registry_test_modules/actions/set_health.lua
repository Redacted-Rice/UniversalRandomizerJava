-- group_test_health.lua
-- Test module for group functionality - health group

local randomizer = require("randomizer")

return {
	id = "health_randomizer",
	name = "Health Randomizer",
	description = "Randomizes entity health",
	groups = { "health", "combat", "stats" },
	defaultSeedOffset = 10,
	author = "Test Author",
	version = "0.1",

	execute = function(context, args)
		local entity = context.entity
		if entity == nil then
			error("No entity found in context")
		end

		-- Set health to a specific value for testing
		entity:setHealth(150)

		return true
	end,
}
