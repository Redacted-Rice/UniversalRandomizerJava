-- Test relative path to unallowed directory using .. from an allowed directory
local relativePath = "../module_fail_cases/unallowed_path.lua"
local f = loadfile(relativePath)
if f then
    return f()
else
    return "loadfile successfully blocked load"
end
