-- enum_expansion_test.lua
-- Tests expanding a Java enum with additional values from Lua

local randomizer = require("randomizer")

return {
	id = "enum_expansion_test",
	name = "Enum Expansion Test",
	description = "Tests expanding a Java enum with additional values from Lua",
	groups = { "test" },
	modifies = { "entity" },
	author = "Test Author",
	version = "1.0.0",

	onLoad = function(context, config)
		-- Extend enum with new types that don't exist in the Java enum
		local extendedEnum = context.extendEnum("EntityType", {
			"PALADIN",
			"NECROMANCER",
			values = {
				PALADIN = 100,
				NECROMANCER = 101
			}
		})

		-- Verify the extension worked with an old and new value
		local hasWarrior = false
		local hasNecromancer = false

		for i = 1, #extendedEnum do
			local value = tostring(extendedEnum[i])
			if value == "WARRIOR" then hasWarrior = true end
			if value == "NECROMANCER" then hasNecromancer = true end
		end

		assert(hasWarrior)
		assert(hasNecromancer)

		-- Verify some value mappings
		assert(extendedEnum.values.WARRIOR == 0)
		assert(extendedEnum.values.PALADIN == 100)
		assert(extendedEnum.values.NECROMANCER == 101)
	end,

	arguments = {
		{
			name = "entityType",
			definition = {
				type = "enum",
				enumName = "EntityType"
			},
			default = "WARRIOR"
		}
	},

	execute = function(context, args)
		-- Verify we received the specific expanded enum value
		local entityType = args.entityType
		assert(entityType == "PALADIN")

		-- Verify the EntityType enum in context has all values
		local EntityType = context.EntityType
		assert(EntityType ~= nil)
		assert(#EntityType == 7)

		local foundValues = {}
		for i = 1, #EntityType do
			foundValues[tostring(EntityType[i])] = true
		end

		-- Check original values
		assert(foundValues["WARRIOR"])
		assert(foundValues["MAGE"])
		assert(foundValues["ROGUE"])
		assert(foundValues["CLERIC"])
		assert(foundValues["RANGER"])

		-- Check expanded values
		assert(foundValues["PALADIN"])
		assert(foundValues["NECROMANCER"])
	end
}
