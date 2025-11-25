-- Test blocking preloading as well
local success, error = pcall(function()
    package.preload["io"] = function() end
end)

if not success then
    return "preload successfully blocked IO injection"
else
    return "preload failed to block IO injection"
end
