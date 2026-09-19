-- 令牌桶：满桶突发，按速率补充；时钟取 Redis TIME。
-- KEYS[1]=hashKey  ARGV[1]=capacity  ARGV[2]=refillRatePerSecond
-- 返回 {allowed, remaining, limit, retryAfterMs}
local t = redis.call('TIME')
local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local key = KEYS[1]
local tokens = tonumber(redis.call('HGET', key, 'tokens'))
local ts = tonumber(redis.call('HGET', key, 'ts'))
if tokens == nil then
  tokens = capacity
  ts = nowMs
else
  local elapsed = math.max(0, nowMs - ts)
  tokens = math.min(capacity, tokens + (elapsed / 1000.0) * rate)
  ts = nowMs
end
local ttlMs = math.max(1000, math.ceil(capacity / rate * 2000))
if tokens >= 1 then
  tokens = tokens - 1
  redis.call('HMSET', key, 'tokens', tostring(tokens), 'ts', ts)
  redis.call('PEXPIRE', key, ttlMs)
  return {1, math.floor(tokens), capacity, 0}
end
local deficit = 1 - tokens
local retry = math.max(1, math.ceil((deficit / rate) * 1000))
redis.call('HMSET', key, 'tokens', tostring(tokens), 'ts', ts)
redis.call('PEXPIRE', key, ttlMs)
return {0, 0, capacity, retry}
