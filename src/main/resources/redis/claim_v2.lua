-- KEYS[1] = userKey
-- KEYS[2] = stockKey
-- KEYS[3] = statusKey
-- ARGV[1] = statusAlready
-- ARGV[2] = statusSoldOut
-- ARGV[3] = statusSuccess
-- ARGV[4] = userClaimedValue
-- ARGV[5] = codeAlready
-- ARGV[6] = codeSoldOut
-- ARGV[7] = codeSuccess

local userKey = KEYS[1]
local stockKey = KEYS[2]
local statusKey = KEYS[3]

local statusAlready = ARGV[1]
local statusSoldOut = ARGV[2]
local statusSuccess = ARGV[3]
local userClaimedValue = ARGV[4]

local codeAlready = tonumber(ARGV[5])
local codeSoldOut = tonumber(ARGV[6])
local codeSuccess = tonumber(ARGV[7])

if redis.call("EXISTS", userKey) == 1 then
    redis.call("SET", statusKey, statusAlready)
    return { codeAlready, -1 }
end

local stockValue = redis.call("GET", stockKey)
local stock = tonumber(stockValue)
if stock == nil or stock <= 0 then
    redis.call("SET", statusKey, statusSoldOut)
    return { codeSoldOut, -1 }
end

local remaining = redis.call("DECR", stockKey)
redis.call("SET", userKey, userClaimedValue)
redis.call("SET", statusKey, statusSuccess)

return { codeSuccess, remaining }
