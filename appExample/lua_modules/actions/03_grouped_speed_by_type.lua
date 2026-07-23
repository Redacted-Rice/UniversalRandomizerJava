-- Example module 3: Randomize speed based on entity type with min/max ranges
-- Takes min/max speed values per type and assigns random values within those ranges

local randomizer = require("randomizer")

return {
	id = "03_grouped_speed_by_type",
	name = "03_grouped_speed_by_type",
	description = "Randomizes speed values based on entity type using min/max ranges",
	groups = { "players" },
	modifies = { "speed" },
	author = "Redacted Rice",
	version = "1.0.0",

	requires = {
		ExampleApp = "0.999.0",
	},

	arguments = {
		{
			name = "speedByType",
			definition = {
				type = "table",
				keyDefinition = {
					type = "enum",
					constraint = "EE_EntityTypes",
				},
				valueDefinition = {
					type = "list",
					elementDefinition = {
						type = "enum",
						constraint = "SpeedClass",
					},
				},
			},
		},
		{
			name = "speedClassPools",
			definition = {
				type = "table",
				keyDefinition = {
					type = "enum",
					constraint = "SpeedClass",
				},
				valueDefinition = {
					type = "list",
					elementDefinition = {
						type = "int",
						constraint = { type = "range", min = 1, max = 100 },
					},
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

		-- Table arguments arrive as plain Lua tables so we need to wrap as Group when using
		-- group based APIs
		randomizer.randomize(entitiesModified, randomizer.group(speedByType), "getType",
			function(entity, speedClass)
				entity.speedClass = speedClass
			end)

		-- Second pass: assign a random speed value based on the assigned speed class
		randomizer.randomize(entitiesModified, randomizer.group(speedClassPools),
			function(entity, speedValue)
				return entity.speedClass
			end, "setSpeed")
	end,
}
