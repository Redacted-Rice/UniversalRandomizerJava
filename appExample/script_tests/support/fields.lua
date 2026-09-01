-- Field spec helpers for script tests. Not a test file. require("support.fields")
local fields = {}

function fields.tags(entries)
	return {
		accessType = "item",
		values = entries or {},
	}
end

function fields.ranks(entries)
	return {
		accessType = "item",
		getter = "getAtRank",
		setter = "setAtRank",
		countGetter = "getRankCounts",
		countSetter = "setRankCounts",
		values = entries or {},
	}
end

function fields.perkRanks(entries)
	local spec = {
		accessType = "item",
		pre = "clearPerkRanks",
	}
	if entries then
		for key, value in pairs(entries) do
			spec[key] = value
		end
	end
	return spec
end

return fields
