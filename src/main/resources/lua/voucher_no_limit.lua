local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId

-- 1.判断库存是否充足
-- if (tonumber(redis.call('get', stockKey)) <= 0) then
--    return 1
-- end

-- 扣库存
-- redis.call('incrby', stockKey, -1)

return 0

-- 正常返回0
-- 库存不足返回1