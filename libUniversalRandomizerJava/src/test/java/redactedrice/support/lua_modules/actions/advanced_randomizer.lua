-- advanced_randomizer.lua
-- A more advanced randomizer demonstrating multiple constraint types

local randomizer = require("randomizer")

return {
	name = "Advanced Entity Randomizer",
	description = "Advanced randomization with grouped pools and multiple constraint types",
	group = "advanced",
	modifies = { "name", "health", "damage", "active" },
	seedOffset = 12345, -- Default seed offset
	author = "Test Author",
	version = "0.1",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	arguments = {
		{
			name = "entityType",
			definition = {
				type = "string",
				constraint = {
					type = "enum",
					values = { "warrior", "mage", "rogue" },
				},
			},
		},
		{
			name = "level",
			definition = {
				type = "integer",
				constraint = {
					type = "discrete_range",
					min = 1,
					max = 100,
					step = 5,
				},
			},
		},
		{
			name = "applyBonus",
			definition = {
				type = "boolean",
			},
		},
	},

	execute = function(context, args)
		-- Seed is already set by the Java wrapper before calling this function
		-- No need to track changes - the wrapper does this automatically

		local entity = context.entity

		if entity == nil then
			error("No entity found in context")
		end

		-- Create grouped pools based on entity type
		local nameGroups = randomizer.group({
			warrior = { "Thorin", "Ragnar", "Conan", "Brock" },
			mage = { "Gandalf", "Merlin", "Morgana", "Zephyr" },
			rogue = { "Shadow", "Whisper", "Viper", "Phantom" },
		})

		local healthGroups = randomizer.group({
			warrior = { 150, 175, 200, 225, 250 },
			mage = { 75, 80, 85, 90, 100 },
			rogue = { 100, 110, 120, 130, 140 },
		})

		local damageGroups = randomizer.group({
			warrior = { 15.0, 18.0, 20.0, 22.0, 25.0 },
			mage = { 25.0, 28.0, 30.0, 33.0, 35.0 },
			rogue = { 12.0, 14.0, 16.0, 18.0, 20.0 },
		})

		-- Use the entityType to select from appropriate pools
		local entityType = args.entityType

		-- Randomize name based on type
		randomizer.randomize({ entity }, nameGroups, function(item)
			return entityType
		end, function(item, newName, index)
			item:setName(newName)
		end)

		-- Randomize health based on type
		randomizer.randomize({ entity }, healthGroups, function(item)
			return entityType
		end, function(item, newHealth, index)
			item:setHealth(newHealth)
		end)

		-- Randomize damage based on type
		randomizer.randomize({ entity }, damageGroups, function(item)
			return entityType
		end, function(item, newDamage, index)
			item:setDamage(newDamage)
		end)

		-- Apply level-based bonus
		local level = args.level
		local healthBonus = level * 2
		local damageBonus = level * 0.5

		entity:setHealth(entity:getHealth() + healthBonus)
		entity:setDamage(entity:getDamage() + damageBonus)

		-- Apply optional bonus
		if args.applyBonus then
			entity:setHealth(math.floor(entity:getHealth() * 1.2))
			entity:setDamage(entity:getDamage() * 1.2)
		end

		entity:setActive(true)

		return true
	end,
}
