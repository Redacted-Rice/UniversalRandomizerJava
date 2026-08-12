-- group_test_health2.lua
-- Test module for group functionality - health group (second module)

local randomizer = require("randomizer")

return {
	id = "health_booster",
	name = "Health Booster",
	description = "Increases entity health",
	groups = { "health" },
	defaultSeedOffset = 30,
	author = "Test Author",
	version = "0.1",

	execute = function(context, args)
		local entity = context.entity
		if entity == nil then
			error("No entity found in context")
		end

		-- Double the current health for testing
		local currentHealth = entity:getHealth()
		entity:setHealth(currentHealth * 2)

		return true
	end,
}
