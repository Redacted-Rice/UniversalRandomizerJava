-- table_of_lists_randomizer.lua
-- Exercises Java supplied table of list arguments wrapped as randomizer.group at runtime

local randomizer = require("randomizer")

return {
	id = "table_of_lists_randomizer",
	name = "Table Of Lists Randomizer",
	description = "Randomizes entity health from table-of-list pools passed from Java",
	groups = { "advanced", "health" },
	author = "Test Author",
	version = "0.1",

	arguments = {
		{
			name = "healthPools",
			definition = {
				type = "table",
				keyDefinition = {
					type = "string",
				},
				valueDefinition = {
					type = "list",
					elementDefinition = {
						type = "integer",
					},
				},
			},
		},
	},

	execute = function(context, args)
		local entities = context.entities
		if entities == nil then
			error("No entities found in context")
		end

		-- Table arguments arrive as plain Lua tables so we wrap before group based randomization
		randomizer.randomize(entities, randomizer.group(args.healthPools), "getCategory", "setHealth")
	end,
}
