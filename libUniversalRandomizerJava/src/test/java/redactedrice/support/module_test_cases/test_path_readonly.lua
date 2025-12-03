-- Test require from unallowed location
-- Try to modify package.path to include unallowed directory, but it should be reset
-- and the require should be blocked
local supportDir = "src/test/java/redactedrice/support"
package.path = package.path .. ";" .. supportDir .. "/?.lua;" .. supportDir .. "/?/init.lua"
local result = require("module_fail_cases")
if result then
    return result
else
    return "require failed"
end
