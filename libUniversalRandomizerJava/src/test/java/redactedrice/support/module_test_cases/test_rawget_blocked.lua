-- Test that rawget is blocked
local success, err = pcall(function()
    return rawget(_G, "print")
end)

if not success then
    return "rawget successfully blocked"
else
    return "rawget was not blocked"
end
