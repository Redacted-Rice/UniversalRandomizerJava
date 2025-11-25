-- Test blocking intentionally unloaded libraries via loader

local success, error = pcall(function()
    package.loaders[#package.loaders + 1] = function() end
end)

if not success then
    return "loaders successfully blocked modification"
else
    return "loaders failed to block modification"
end
