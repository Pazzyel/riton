local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]
local day = ARGV[4]
local limit = tonumber(ARGV[5])

-- local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'order:daily' .. day .. voucherId

-- 1.判断库存是否充足 (普通订单不判断库存）
-- if (tonumber(redis.call('get', stockKey)) <= 0) then
--    return 1
-- end
-- 2.判断用户是否超出限额
if ((not redis.call('exists', orderKey)) or (not redis.call('hexists', orderKey, userId))) then
    redis.call('hset', orderKey, userId, 0)
    redis.call('expire', orderKey, 172800) -- 2天后过期
end
local count = tonumber(redis.call('hget', orderKey, userId)) or 0
if (count >= limit) then
    return 2
end

-- 扣库存
-- redis.call('incrby', stockKey, -1)
-- 添加订单
redis.call('hset', orderKey, userId, count + 1)

return 0

-- 正常返回0
-- 库存不足返回1
-- 超出限额返回2