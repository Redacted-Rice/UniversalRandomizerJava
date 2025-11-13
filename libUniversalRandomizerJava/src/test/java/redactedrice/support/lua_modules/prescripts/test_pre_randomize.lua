-- Test helper prescript that should run once per randomize batch
return {
	name = "Test Pre Randomize Script",
	description = "Marks that pre-randomize scripts executed for tests",
	when = "randomize",

	execute = function(context)
		-- returning a value makes it easy to see the result from Java
		return "pre_randomize_ran"
	end,
}
