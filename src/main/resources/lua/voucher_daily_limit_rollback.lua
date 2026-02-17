local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]
local day = ARGV[4]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:daily' .. day .. voucherId

redis.call('incrby', stockKey, 1)

local count = redis.call('hget', orderKey, userId)
if (count and tonumber(count) > 0) then
    local newCount = redis.call('hincrby', orderKey, userId, -1)
    if (newCount <= 0) then
        redis.call('hdel', orderKey, userId)
    end
end

return 0
