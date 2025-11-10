-- PRE-MODULE script: Take snapshot before each module runs
-- Timing: Runs before each randomization module executes
-- Purpose: Captures current state to detect changes after module completes

return {
	name = "changedetector_snapshot",
	description = "Take snapshot before each module",
	when = "pre-module",

	execute = function(context)
		local changedetector = require("randomizer").changedetector
		-- Take snapshots of all monitored objects
		changedetector.takeSnapshots()
	end,
}

