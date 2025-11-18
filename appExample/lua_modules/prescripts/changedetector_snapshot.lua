-- Takes a snapshot for change detection before each module
return {
	name = "changedetector_snapshot",
	description = "Take snapshot before each module",
	when = "module",
	author = "Example Author",
	version = "1.0.0",
	requires = {
		UniversalRandomizerJava = "0.5.0",
	},

	execute = function(context)
		local changedetector = require("randomizer").changedetector
		changedetector.takeSnapshots()
	end,
}

