-- Runs after each module execution to validate per-module postscripts
return {
	name = "Test Post Module Script",
	description = "Ensures module-level postscripts are invoked",
	when = "module",

	execute = function(context)
		return "post_module_ran"
	end,
}
