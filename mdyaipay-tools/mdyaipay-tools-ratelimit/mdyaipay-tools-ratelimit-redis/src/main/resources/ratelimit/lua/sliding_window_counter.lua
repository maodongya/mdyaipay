-- 滑动窗口计数：KEYS[1]=hash  ARGV=limit seg elapsed segmentMs expireMs
local limit = tonumber(ARGV[1])
local seg = tonumber(ARGV[2])
local elapsed = tonumber(ARGV[3])
local segmentMs = tonumber(ARGV[4])
local h = redis.call('HMGET', KEYS[1], 'seg', 'cur', 'prev')
local curSeg = tonumber(h[1] or '-1')
local cur = tonumber(h[2] or '0')
local prev = tonumber(h[3] or '0')
local dirty = false
if curSeg ~= seg then
  prev = (seg - curSeg == 1) and cur or 0
  cur, curSeg, dirty = 0, seg, true
end
local estimated = cur + prev * (1 - elapsed / segmentMs)
if estimated < limit then
  cur = cur + 1
  redis.call('HMSET', KEYS[1], 'seg', curSeg, 'cur', cur, 'prev', prev)
  redis.call('PEXPIRE', KEYS[1], ARGV[5])
  return {1, math.max(0, limit - math.ceil(estimated + 1)), limit, 0}
end
if dirty then
  redis.call('HMSET', KEYS[1], 'seg', curSeg, 'cur', cur, 'prev', prev)
  redis.call('PEXPIRE', KEYS[1], ARGV[5])
end
return {0, 0, limit, math.max(1, segmentMs - elapsed)}
