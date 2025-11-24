-- Script that allocates 200 MB of memory which is significantly above
-- the default 100MB limit
-- This should trigger memory limit enforcement with default 100MB limit
-- Each entry is ~1000 bytes, so 200000 entries = ~200MB
local largeTable = {}
for i = 1, 200000 do -- 200k * 1 kB = 200 MB
    largeTable[i] = string.rep('x', 1024) -- 1 kB entry
end
return 'allocated 200 MB of memory'
