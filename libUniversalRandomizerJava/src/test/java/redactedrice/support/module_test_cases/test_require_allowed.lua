-- Test require from allowed location
local result = require("includetest")
if result and result.message then
    return result.message
else
    return "require failed"
end
