-- Takes a snapshot for change detection before each module
return {
	 id = "changedetector_snapshot",
	name = "changedetector_snapshot",
	description = "Take snapshot before each module",
	when = "module",
	author = "Redacted Rice",
	version = "1.0.0",

	requires = {
		ExampleApp = "1.0.0",
		changedetector_setup = "1.0.0",
	},

	execute = function(context)
		local changedetector = require("randomizer").changedetector
		changedetector.takeSnapshots()
	end,
}

