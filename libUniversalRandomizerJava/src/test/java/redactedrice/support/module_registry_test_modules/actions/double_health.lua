-- group_test_health2.lua
-- Test module for group functionality - health group (second module)

local randomizer = require("randomizer")

return {
	name = "Health Booster",
	description = "Increases entity health",
	group = "health",
	modifies = "health",
	seedOffset = 30,
	author = "Test Author",
	version = "0.1",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

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
