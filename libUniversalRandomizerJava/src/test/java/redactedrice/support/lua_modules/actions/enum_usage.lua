-- enum_usage.lua
-- Uses enums registered by another module in onLoad

local randomizer = require("randomizer")

return {
	name = "Enum Usage",
	description = "Uses enums registered by another module in onLoad",
	group = "utils",
	modifies = {},
	author = "Test Author",
	version = "0.1",

	execute = function(context, args)
		-- Test TestPriority (case 1: array with explicit values subtable)
		local TestPriority = context.TestPriority
		if not TestPriority then
			error("TestPriority enum not found in context")
		end
		local val1 = TestPriority[1]
		if val1 == nil then
			error("TestPriority[1] is nil - enum table may not be structured correctly")
		end
		assert(tostring(val1) == "LOW", "TestPriority: Index 1 should be LOW, got: " .. tostring(val1))
		assert(tostring(TestPriority[2]) == "MEDIUM", "TestPriority: Index 2 should be MEDIUM")
		assert(tostring(TestPriority[3]) == "HIGH", "TestPriority: Index 3 should be HIGH")
		assert(TestPriority.values ~= nil, "TestPriority: Should have values subtable")
		assert(TestPriority.values.LOW == 1, "TestPriority: LOW should have value 1")
		assert(TestPriority.values.MEDIUM == 50, "TestPriority: MEDIUM should have value 50")
		assert(TestPriority.values.HIGH == 100, "TestPriority: HIGH should have value 100")
		print("  [execute] TestPriority enum verified (case 1: array with explicit values)")

		-- Test TestPriority2 (case 2: array with implicit values 0, 1, 2)
		local TestPriority2 = context.TestPriority2
		if not TestPriority2 then
			error("TestPriority2 enum not found in context")
		end
		assert(tostring(TestPriority2[1]) == "LOW", "TestPriority2: Index 1 should be LOW")
		assert(tostring(TestPriority2[2]) == "MEDIUM", "TestPriority2: Index 2 should be MEDIUM")
		assert(tostring(TestPriority2[3]) == "HIGH", "TestPriority2: Index 3 should be HIGH")
		assert(TestPriority2.values ~= nil, "TestPriority2: Should have values subtable")
		assert(TestPriority2.values.LOW == 0, "TestPriority2: LOW should have value 0")
		assert(TestPriority2.values.MEDIUM == 1, "TestPriority2: MEDIUM should have value 1")
		assert(TestPriority2.values.HIGH == 2, "TestPriority2: HIGH should have value 2")
		print("  [execute] TestPriority2 enum verified (case 2: array with implicit values)")

		-- Test TestPriority3 (case 3: map-based enum)
		local TestPriority3 = context.TestPriority3
		if not TestPriority3 then
			error("TestPriority3 enum not found in context")
		end
		-- Map-based enum should still have array indices (based on insertion order)
		assert(tostring(TestPriority3[1]) == "LOW", "TestPriority3: Index 1 should be LOW")
		assert(tostring(TestPriority3[2]) == "MEDIUM", "TestPriority3: Index 2 should be MEDIUM")
		assert(tostring(TestPriority3[3]) == "HIGH", "TestPriority3: Index 3 should be HIGH")
		assert(TestPriority3.values ~= nil, "TestPriority3: Should have values subtable")
		assert(TestPriority3.values.LOW == 1, "TestPriority3: LOW should have value 1")
		assert(TestPriority3.values.MEDIUM == 50, "TestPriority3: MEDIUM should have value 50")
		assert(TestPriority3.values.HIGH == 100, "TestPriority3: HIGH should have value 100")
		print("  [execute] TestPriority3 enum verified (case 3: map-based enum)")

		-- Test setting enum value on Java object to verify de-lua-ifying conversion
		local testEntity = context.testEntity
		if testEntity then
			-- Set TestPriority using string value - should be passed as string to Java
			testEntity:setPriority("MEDIUM")
			print("  [execute] Set testEntity.priority = 'MEDIUM' (should pass as string to Java)")
		end

		print("  [execute] Enum Usage module executed successfully")
		print(
			"  [execute] All three enum types (TestPriority, TestPriority2, TestPriority3) are available and have correct structure"
		)
	end,
}

