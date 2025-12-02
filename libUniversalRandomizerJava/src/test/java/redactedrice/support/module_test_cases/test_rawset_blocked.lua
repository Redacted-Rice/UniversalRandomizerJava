-- Test that rawset is blocked
local success, err = pcall(function()
    return rawset(_G, "maliciousGlobal", "value")
end)

if not success then
    return "rawset successfully blocked"
else
    return "rawset was not blocked"
end
