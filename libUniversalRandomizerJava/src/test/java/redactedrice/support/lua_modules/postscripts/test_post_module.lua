-- Runs after each module execution to validate per-module postscripts
return {
	name = "Test Post Module Script",
	description = "Ensures module-level postscripts are invoked",
	when = "module",
	author = "Test Author",
	version = "0.1",

	execute = function(context)
		return "post_module_ran"
	end,
}
