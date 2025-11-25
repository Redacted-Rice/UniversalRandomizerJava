-- Test: Attempt to require io module
local success, error = pcall(function()
    require("io")
end)

if not success then
    return "require successfully blocked IO"
else
    return "require failed to block IO"
end
