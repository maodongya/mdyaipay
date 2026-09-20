-- 滑动窗口日志：KEYS[1]=zset  ARGV=limit nowMs windowStart member expireMs；拒绝时第 4 字段为最旧 score
redis.call('ZREMRANGEBYSCORE', KEYS[1], '0', ARGV[3])
local limit = tonumber(ARGV[1])
local count = redis.call('ZCARD', KEYS[1])
if count < limit then
  redis.call('ZADD', KEYS[1], ARGV[2], ARGV[4])
  redis.call('PEXPIRE', KEYS[1], ARGV[5])
  return {1, limit - count - 1, limit, 0}
end
return {0, 0, limit, tonumber(redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')[2])}
