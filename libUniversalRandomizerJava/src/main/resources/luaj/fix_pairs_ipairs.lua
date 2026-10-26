-- LuaJ pairs/ipairs ignore __pairs and __ipairs. Wire them up like Lua 5.2 so they behave as expected
local raw_pairs = pairs
local raw_ipairs = ipairs

function pairs(t)
	local mt = getmetatable(t)
	if mt ~= nil then
		local iterator = mt.__pairs
		if iterator ~= nil then
			return iterator(t)
		end
	end
	return raw_pairs(t)
end

function ipairs(t)
	local mt = getmetatable(t)
	if mt ~= nil then
		local iterator = mt.__ipairs
		if iterator ~= nil then
			return iterator(t)
		end
	end
	return raw_ipairs(t)
end
