-- Example module 3: Randomize speed based on entity type with min/max ranges
-- Takes min/max speed values per type and assigns random values within those ranges

local randomizer = require("randomizer")

return {
	name = "03_grouped_speed_by_type",
	description = "Randomizes speed values based on entity type using min/max ranges",
	group = "players",
	modifies = { "speed" },
	author = "Redacted Rice",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	arguments = {
		{
			name = "speedByType",
			definition = {
				type = "group",
				keyDefinition = {
					type = "enum",
					constraint = "EE_EntityTypes",
				},
				listElementDefinition = {
					type = "enum",
					constraint = "SpeedClass",
				},
			},
		},
		{
			name = "speedClassPools",
			definition = {
				type = "group",
				keyDefinition = {
					type = "enum",
					constraint = "SpeedClass",
				},
				listElementDefinition = {
					type = "int",
					min = 1,
					max = 100,
				},
			},
		},
	},

	onLoad = function(context)
		-- Register a custom enum with integer values
		context.registerEnum("SpeedClass", {
			"SLOW",
			"AVERAGE",
			"FAST",
		})
	end,

	execute = function(context, arguments)
		local entitiesModified = context.entitiesModified

		-- Get arguments. These should always be provided
		local speedByType = arguments.speedByType
		local speedClassPools = arguments.speedClassPools

		-- Assign a random speed class to each entity based on its type
		randomizer.randomize(entitiesModified, speedByType, "getType", function(entity, speedClass)
			entity.speedClass = speedClass
		end)

		-- Second pass: assign a random speed value based on the assigned speed class
		-- Use a custom key function that looks up the speed class we stored
		randomizer.randomize(entitiesModified, speedClassPools, function(entity, speedValue)
			return entity.speedClass
		end, "setSpeed")
	end,
}
