-- enhanced_randomizer.lua
-- Comprehensive example demonstrating new features:
-- - Group and modifies metadata
-- - Enum parameters
-- - List parameters
-- - Map parameters
-- - Default values

local randomizer = require("randomizer")

return {
	name = "Enhanced Entity Randomizer",
	description = "Demonstrates all new parameter types and metadata features",
	group = { "gameplay", "advanced" },
	modifies = { "stats", "appearance", "loot" },
	seedOffset = 77777,
	author = "Test Author",
	version = "0.1",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	arguments = {
		-- Enum parameter (references context enum)
		{
			name = "entityType",
			definition = {
				type = "enum",
				-- constrain to a specific enum
				constraint = "EntityType",
			},
			default = "WARRIOR",
		},

		-- List parameter (list of integers)
		{
			name = "statBonuses",
			definition = {
				type = "list",
				elementDefinition = {
					type = "integer",
					-- default to constraint any
				},
			},
			default = { 0, 0, 0 }, -- Default bonuses
		},

		-- Map parameter (string -> integer)
		{
			name = "attributeModifiers",
			definition = {
				type = "map",
				keyDefinition = {
					type = "string",
					-- default to constraint any
				},
				valueDefinition = {
					type = "integer",
					constraint = {
						type = "range",
						min = 1,
						max = 100,
					},
				},
			},
			default = { health = 50, damage = 10 },
		},

		-- Simple boolean with default
		{
			name = "applyRandomness",
			definition = {
				type = "boolean",
				-- no constraint needed for boolean
			},
			default = true,
		},

		-- Integer with default
		{
			name = "powerLevel",
			definition = {
				type = "integer",
				constraint = {
					type = "range",
					min = 1,
					max = 100,
				},
			},
			default = 50,
		},
	},

	execute = function(context, args)
		local entity = context.entity

		if entity == nil then
			error("No entity found in context")
		end

		-- Access enum value from args
		local entityType = args.entityType
		logger.info("Randomizing entity of type: " .. tostring(entityType))

		-- Apply stat bonuses from list (now a Lua table)
		local statBonuses = args.statBonuses
		if statBonuses and #statBonuses >= 2 then
			entity:setHealth(entity:getHealth() + statBonuses[1])
			entity:setDamage(entity:getDamage() + statBonuses[2])
		end

		-- Apply attribute modifiers from map
		local modifiers = args.attributeModifiers
		if modifiers then
			for key, value in pairs(modifiers) do
				if key == "health" then
					entity:setHealth(entity:getHealth() + value)
				elseif key == "damage" then
					entity:setDamage(entity:getDamage() + value)
				end
			end
		end

		-- Apply power level scaling
		local powerLevel = args.powerLevel
		local scaleFactor = powerLevel / 50.0 -- 50 is the default
		entity:setHealth(math.floor(entity:getHealth() * scaleFactor))
		entity:setDamage(entity:getDamage() * scaleFactor)

		-- Apply randomness if enabled
		if args.applyRandomness then
			-- Use utils.randomElement instead
			local bonuses = { 5, 10, 15, 20 }
			local randomBonus = bonuses[math.random(#bonuses)]
			entity:setHealth(entity:getHealth() + randomBonus)
		end

		-- Set name based on entity type
		local namesByType = {
			WARRIOR = { "Thorin", "Conan", "Ragnar" },
			MAGE = { "Gandalf", "Merlin", "Morgana" },
			ROGUE = { "Shadow", "Viper", "Phantom" },
			CLERIC = { "Aria", "Benedict", "Celestia" },
			RANGER = { "Legolas", "Artemis", "Strider" },
		}

		local names = namesByType[entityType] or { "Unknown" }
		entity:setName(names[math.random(#names)])

		return true
	end,
}
