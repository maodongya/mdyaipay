-- 滑动窗口计数：当前段 + 上一段权重插值。
-- KEYS[1]=hashKey  ARGV[1]=limit  ARGV[2]=windowMs  ARGV[3]=segments
-- 返回 {allowed, remaining, limit, retryAfterMs}
local t = redis.call('TIME')
local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local limit = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local segments = tonumber(ARGV[3])
local segmentMs = math.max(1, math.floor(windowMs / segments))
local seg = math.floor(nowMs / segmentMs)
local elapsed = nowMs % segmentMs
local key = KEYS[1]
local curSeg = tonumber(redis.call('HGET', key, 'seg') or '-1')
local cur = tonumber(redis.call('HGET', key, 'cur') or '0')
local prev = tonumber(redis.call('HGET', key, 'prev') or '0')
if curSeg ~= seg then
  local gap = seg - curSeg
  if gap == 1 then
    prev = cur
  else
    prev = 0
  end
  cur = 0
  curSeg = seg
end
local weight = 1.0 - (elapsed / segmentMs)
local estimated = cur + prev * weight
if estimated < limit then
  cur = cur + 1
  redis.call('HMSET', key, 'seg', curSeg, 'cur', cur, 'prev', prev)
  redis.call('PEXPIRE', key, windowMs * 2)
  local rem = math.max(0, limit - math.ceil(estimated + 1))
  return {1, rem, limit, 0}
end
local retry = math.max(1, segmentMs - elapsed)
redis.call('HMSET', key, 'seg', curSeg, 'cur', cur, 'prev', prev)
redis.call('PEXPIRE', key, windowMs * 2)
return {0, 0, limit, retry}
