-- Test that new globals cannot be created but existing globals are accessible
local results = {}

--  Attempt to create a new global
local success1, _ = pcall(function()
    newGlobal = "should_be_blocked"
end)
if not success1 then
    table.insert(results, "new globals blocked")
end

-- Verify we can access existing globals set by the sandbox
if testGlobal ~= nil and testGlobal == "test_value_123" then
    table.insert(results, "testGlobal accessible")
end

if testGlobalNumber ~= nil and testGlobalNumber == 42 then
    table.insert(results, "testGlobalNumber accessible")
end

return table.concat(results, "|")
