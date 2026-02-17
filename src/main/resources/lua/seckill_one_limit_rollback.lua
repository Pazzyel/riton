local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:forever' .. voucherId

-- 补回库存
redis.call('incrby', stockKey, 1)
-- 移除用户集合
redis.call('srem', orderKey, userId)

return 0

-- 正常返回0
-- 库存不足返回1
-- 重复下单返回2