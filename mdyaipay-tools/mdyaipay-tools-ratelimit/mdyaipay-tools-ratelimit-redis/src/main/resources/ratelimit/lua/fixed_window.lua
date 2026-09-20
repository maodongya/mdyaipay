-- 固定窗口：KEYS[1]=bucketKey  ARGV[1]=limit  ARGV[2]=expireMs
local limit = tonumber(ARGV[1])
local count = redis.call('INCR', KEYS[1])
if count == 1 then
  redis.call('PEXPIRE', KEYS[1], ARGV[2])
end
if count <= limit then
  return {1, limit - count, limit, 0}
end
return {0, 0, limit, math.max(1, redis.call('PTTL', KEYS[1]))}
