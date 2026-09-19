-- 固定窗口：时钟对齐窗口起点，INCR + PEXPIRE。
-- KEYS[1]=baseKey  ARGV[1]=limit  ARGV[2]=windowMs
-- 返回 {allowed, remaining, limit, retryAfterMs}
local t = redis.call('TIME')
local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local limit = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local windowStart = math.floor(nowMs / windowMs) * windowMs
local bucketKey = KEYS[1] .. ':' .. tostring(windowStart)
local count = redis.call('INCR', bucketKey)
if count == 1 then
  redis.call('PEXPIRE', bucketKey, windowMs)
end
if count <= limit then
  return {1, limit - count, limit, 0}
end
local ttl = redis.call('PTTL', bucketKey)
if ttl < 1 then
  ttl = 1
end
return {0, 0, limit, ttl}
