-- Takes a snapshot for change detection before each module
return {
	name = "changedetector_snapshot",
	description = "Take snapshot before each module",
	when = "module",

	execute = function(context)
		local changedetector = require("randomizer").changedetector
		changedetector.takeSnapshots()
	end,
}

