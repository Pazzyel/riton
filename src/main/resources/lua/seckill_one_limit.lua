local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:forever' .. voucherId

-- 1.判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end
-- 2.判断用户是否重复下单
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 扣库存
redis.call('incrby', stockKey, -1)
-- 添加用户集合
redis.call('sadd', orderKey, userId)

return 0

-- 正常返回0
-- 库存不足返回1
-- 重复下单返回2