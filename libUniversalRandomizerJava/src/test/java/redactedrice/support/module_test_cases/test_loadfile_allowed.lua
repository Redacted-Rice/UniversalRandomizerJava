-- Test loadfile from allowed location
-- Load from modules/includetest in allowed directory
-- Since dofile is blocked we must load before executing, so this tests both load and execute
local allowedPath = "src/test/java/redactedrice/support/modules/includetest/test_file.lua"
local f = loadfile(allowedPath)
if f then
    return f()
else
    return "loadfile failed: " .. allowedPath
end
