-- simple_randomizer.lua
-- A simple randomizer that modifies entity attributes

local randomizer = require("randomizer")

return {
	name = "Simple Entity Randomizer",
	description = "Randomizes entity name, health, and damage values",
	seedOffset = 42, -- Default seed offset

	arguments = {
		{
			name = "healthMin",
			definition = {
				type = "integer",
				constraint = {
					type = "range",
					min = 1,
					max = 1000,
				},
			},
		},
		{
			name = "healthMax",
			definition = {
				type = "integer",
				constraint = {
					type = "range",
					min = 1,
					max = 1000,
				},
			},
		},
		{
			name = "damageMultiplier",
			definition = {
				type = "double",
				constraint = {
					type = "range",
					min = 0.1,
					max = 10.0,
				},
			},
		},
	},

	execute = function(context, args)
		-- Seed is already set by the Java wrapper before calling this function
		-- No need to track changes - the wrapper does this automatically

		-- Get the entity from context
		local entity = context.entity

		if entity == nil then
			error("No entity found in context")
		end

		-- Create pools for randomization
		local namePool = randomizer.list({ "Warrior", "Mage", "Archer", "Knight", "Rogue" })
		local healthPool = randomizer.list({
			args.healthMin,
			args.healthMax,
			math.floor((args.healthMin + args.healthMax) / 2),
		})

		-- Randomize the entity's name
		randomizer.randomize({ entity }, namePool, function(item, newName, index)
			item:setName(newName)
		end)

		-- Randomize health
		randomizer.randomize({ entity }, healthPool, function(item, newHealth, index)
			item:setHealth(newHealth)
		end)

		-- Modify damage based on multiplier
		local currentDamage = entity:getDamage()
		local newDamage = currentDamage * args.damageMultiplier
		entity:setDamage(newDamage)

		-- Set active status based on health
		if entity:getHealth() < 50 then
			entity:setActive(false)
		else
			entity:setActive(true)
		end

		return true
	end,
}
