-- Test blocking intentionally unloaded libraries via loaded direct access
local io_module = package.loaded["io"]

if io_module == nil then
    return "loaded successfully blocked IO"
else
    return "loaded failed to block IO"
end
