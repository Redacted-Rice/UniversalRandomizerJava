-- flag_enum.lua
-- Tests flag enum usage with non-sequential values

local randomizer = require("randomizer")

return {
	name = "Flag Enum",
	description = "Tests flag enum usage with non-sequential values",
	groups = { "utils" },
	modifies = { "entity" },
	author = "Test Author",
	version = "0.1",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context, args)
		-- The FlagEnum should be available here because it was registered from Java
		local FlagEnum = context.FlagEnum

		if not FlagEnum then
			error("FlagEnum not found in context")
		end

		-- Verify the enum has the expected structure
		-- Sequential array should contain all enum names
		assert(tostring(FlagEnum[1]) == "FLAG_NONE", "Index 1 should be FLAG_NONE")
		assert(tostring(FlagEnum[2]) == "FLAG_ONE", "Index 2 should be FLAG_ONE")
		assert(tostring(FlagEnum[3]) == "FLAG_TWO", "Index 3 should be FLAG_TWO")
		assert(tostring(FlagEnum[4]) == "FLAG_FOUR", "Index 4 should be FLAG_FOUR")
		assert(tostring(FlagEnum[5]) == "FLAG_EIGHT", "Index 5 should be FLAG_EIGHT")

		-- Verify values subtable has correct flag values (not ordinals)
		assert(FlagEnum.values ~= nil, "Should have values subtable")
		assert(FlagEnum.values.FLAG_NONE == 0, "FLAG_NONE should have value 0")
		assert(FlagEnum.values.FLAG_ONE == 1, "FLAG_ONE should have value 1")
		assert(FlagEnum.values.FLAG_TWO == 2, "FLAG_TWO should have value 2")
		assert(FlagEnum.values.FLAG_FOUR == 4, "FLAG_FOUR should have value 4 (not 3)")
		assert(FlagEnum.values.FLAG_EIGHT == 8, "FLAG_EIGHT should have value 8 (not 4)")

		-- Test setting enum value on Java object to verify de-lua-ifying conversion
		local testEntity = context.testEntity
		if testEntity then
			-- Set FlagEnum using string value - should be converted to Java FlagEnum.FLAG_FOUR
			testEntity:setFlag("FLAG_FOUR")
			print("  [execute] Set testEntity.flag = 'FLAG_FOUR' (should convert to Java FlagEnum.FLAG_FOUR)")
		end

		print("  [execute] Flag Enum module executed successfully")
		print("  [execute] FlagEnum has correct sequential array and flag values")
	end,
}

