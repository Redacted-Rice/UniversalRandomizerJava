-- Test that collectgarbage is blocked
local success, error = pcall(function()
    collectgarbage("collect")
end)

if not success then
    return "collect garbage successfully blocked"
else
    return "collect garbage failed to be blocked"
end
