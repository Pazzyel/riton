local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

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
-- 添加订单
redis.call('sadd', orderKey, userId)
-- 发送到消息队列Redis,Stream
-- 注意，在使用 XREADGROUP 等命令之前，需要先创建消费者组，重启Redis会丢失之前的消费者组
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0

-- 正常返回0
-- 库存不足返回1
-- 重复下单返回2