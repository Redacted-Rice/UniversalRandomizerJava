-- Runs before each module execution to validate per-module hooks
return {
	name = "Test Pre Module Script",
	description = "Ensures module-level prescripts are invoked",
	when = "module",

	execute = function(context)
		return "pre_module_ran"
	end,
}
