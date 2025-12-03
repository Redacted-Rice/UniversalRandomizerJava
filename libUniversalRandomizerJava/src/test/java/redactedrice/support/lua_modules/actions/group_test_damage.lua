-- group_test_damage.lua
-- Test module for group functionality - damage group

local randomizer = require("randomizer")

return {
	name = "Damage Randomizer",
	description = "Randomizes entity damage",
	group = "damage",
	seedOffset = 20,
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

		-- Set damage to a specific value for testing
		entity:setDamage(75.5)

		return true
	end,
}
