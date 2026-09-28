-- ============================================================
-- 秒杀原子脚本：一人一单校验 + 库存扣减 + 下单标记，一次原子完成
-- ------------------------------------------------------------
-- KEYS[1] 秒杀库存 key        seckill:stock:{voucherId}
-- KEYS[2] 下单标记集合 key     seckill:order:set:{voucherId}
-- ARGV[1] 用户ID（集合成员；一人一单以用户为粒度，而非订单ID）
-- ARGV[2] 集合过期时间（秒），对齐秒杀结束时间，避免长期占用内存
-- 返回：1 扣减成功；-1 库存不足；-2 重复下单
-- ============================================================

-- 1. 一人一单校验：该用户已抢过，直接拒绝
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return -2
end

-- 2. 库存校验：key 不存在视为 0（未预热也按无库存处理，由调用方先触发预热）
local stock = tonumber(redis.call('get', KEYS[1]) or '-1')
if stock <= 0 then
    return -1
end

-- 3. 原子扣减库存 + 记录下单标记（同一脚本内，不存在中间态）
redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
redis.call('expire', KEYS[2], tonumber(ARGV[2]))

return 1
