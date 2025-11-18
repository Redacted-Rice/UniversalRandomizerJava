-- enum_onload.lua
-- Registers enums in onLoad for use by other modules

local randomizer = require("randomizer")

return {
	name = "Enum OnLoad",
	description = "Registers test enums in onLoad for use by other modules",
	group = "utils",
	modifies = {},
	author = "Test Author",
	version = "0.1",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	onLoad = function(context)
		-- Register a custom enum with integer values
		context.registerEnum("TestPriority", {
			"LOW",
			"MEDIUM",
			"HIGH",
			values = {
				LOW = 1,
				MEDIUM = 50,
				HIGH = 100,
			},
		})
		-- Register with implicit values (0, 1, 2)
		context.registerEnum("TestPriority2", {
			"LOW",
			"MEDIUM",
			"HIGH",
		})
		-- Register based on a map of name -> integer value
		context.registerEnum("TestPriority3", {
			LOW = 1,
			MEDIUM = 50,
			HIGH = 100,
		})

		print("  [onLoad] Registered TestPriority, TestPriority2, and TestPriority3 enums")
	end,

	execute = function(context, args)
		-- This module doesn't do anything during execution
		-- It's just for registering the enum in onLoad
		print("  [execute] Enum OnLoad module executed (enums should be registered)")
	end,
}

