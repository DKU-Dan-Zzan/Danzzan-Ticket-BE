-- KEYS[1] = ticket:event:{eventId}:remaining
-- KEYS[2] = ticket:event:{eventId}:status
-- KEYS[3] = ticket:reserve:lock:{eventId}:{userId}
-- ARGV[1] = lock TTL seconds ("3")
--
-- Returns: >=0 newRemaining (성공), -1 미오픈, -2 중복클릭, -3 매진

local status = redis.call('GET', KEYS[2])
if status ~= 'OPEN' then return -1 end

if redis.call('EXISTS', KEYS[3]) == 1 then return -2 end

local remaining = tonumber(redis.call('GET', KEYS[1]))
if remaining == nil or remaining <= 0 then return -3 end

local newRemaining = redis.call('DECR', KEYS[1])
if newRemaining < 0 then
    redis.call('INCR', KEYS[1])
    return -3
end

redis.call('SET', KEYS[3], '1', 'EX', tonumber(ARGV[1]))
return newRemaining
