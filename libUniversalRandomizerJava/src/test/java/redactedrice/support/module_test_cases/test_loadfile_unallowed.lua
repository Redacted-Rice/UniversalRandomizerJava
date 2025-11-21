-- Test loadfile/execute from unallowed location
local unallowedPath = "src/test/java/redactedrice/support/module_fail_cases/unallowed_path.lua"
local f = loadfile(unallowedPath)
if f then
    return f()
else
    return "loadfile failed"
end
