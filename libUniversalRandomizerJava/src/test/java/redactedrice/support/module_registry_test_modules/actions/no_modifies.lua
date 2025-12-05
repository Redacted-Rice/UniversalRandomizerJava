-- no_modifies.lua
-- Test module without modifies field to verify it's allowed

local randomizer = require("randomizer")

return {
	name = "No Modifies Test",
	description = "Tests that missing modifies field is allowed",
	groups = { "test" },
	seedOffset = 88,
	author = "Test Author",
	version = "0.1",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context, args)
		-- This module doesn't specify what it modifies
		return true
	end,
}
