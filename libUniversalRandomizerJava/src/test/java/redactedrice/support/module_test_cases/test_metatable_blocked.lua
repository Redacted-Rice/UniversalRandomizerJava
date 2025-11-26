-- Test: Verify that getmetatable and setmetatable work for user tables
-- but globals metatable manipulation is protected
local results = {}

-- Test that getmetatable and setmetatable work for user tables
local success1, _ = pcall(function()
    local mt = getmetatable({})
    table.insert(results, "getmetatable works")
end)

local success2, _ = pcall(function()
    setmetatable({}, {})
    table.insert(results, "setmetatable works")
end)

-- Test that we cannot modify the globals metatable
local success3, _ = pcall(function()
    setmetatable(_G, {})
end)
if not success3 then
    table.insert(results, "globals metatable protected")
end

-- Test that we cannot remove the globals metatable
local success4, _ = pcall(function()
    setmetatable(_G, nil)
end)
if not success4 then
    table.insert(results, "globals metatable removal blocked")
end

return table.concat(results, "|")
