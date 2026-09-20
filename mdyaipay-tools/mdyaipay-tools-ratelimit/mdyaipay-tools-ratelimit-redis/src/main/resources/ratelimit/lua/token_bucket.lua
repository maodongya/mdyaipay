-- 令牌桶：KEYS[1]=hash  ARGV=capacity rate nowMs ttlMs
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local nowMs = tonumber(ARGV[3])
local h = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(h[1])
if tokens then
  tokens = math.min(capacity, tokens + math.max(0, nowMs - tonumber(h[2])) / 1000 * rate)
else
  tokens = capacity
end
local allowed, rem, retry
if tokens >= 1 then
  tokens = tokens - 1
  allowed, rem, retry = 1, math.floor(tokens), 0
else
  allowed, rem, retry = 0, 0, math.max(1, math.ceil((1 - tokens) / rate * 1000))
end
redis.call('HMSET', KEYS[1], 'tokens', tokens, 'ts', nowMs)
redis.call('PEXPIRE', KEYS[1], ARGV[4])
return {allowed, rem, capacity, retry}
