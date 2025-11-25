-- Test blocking intentionally unloaded libraries loaded via injection
local success, error = pcall(function()
    package.loaded["io"] = {}
end)

if not success then
    return "loaded successfully blocked IO injection"
else
    return "loaded failed to block IO injection"
end
