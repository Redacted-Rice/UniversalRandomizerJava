-- Test helper postscript that should run once per randomize batch
return {
	name = "Test Post Randomize Script",
	description = "Marks that post-randomize scripts executed for tests",
	when = "randomize",

	execute = function(context)
		return "post_randomize_ran"
	end,
}
