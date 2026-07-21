local challengeKey = KEYS[1]
local codeKey = KEYS[2]
local tokenKey = KEYS[3]

local userId = ARGV[1]
local waitStatus = ARGV[2]
local verifiedStatus = ARGV[3]
local submittedCode = ARGV[4]
local secureToken = ARGV[5]
local secureContextJson = ARGV[6]
local tokenTtlSeconds = tonumber(ARGV[7])
local maxAttempts = tonumber(ARGV[8])

if redis.call('exists', challengeKey) == 0 then
    return '0:'
end

if redis.call('hget', challengeKey, 'userId') ~= userId then
    return '1:'
end

local status = redis.call('hget', challengeKey, 'status')
if status == verifiedStatus then
    return '7:' .. (redis.call('hget', challengeKey, 'secureToken') or '')
end
if status ~= waitStatus then
    return '2:'
end

local expectedCode = redis.call('get', codeKey)
if not expectedCode then
    return '3:'
end

if expectedCode ~= submittedCode then
    local attempts = redis.call('hincrby', challengeKey, 'failedAttempts', 1)
    if attempts >= maxAttempts then
        redis.call('del', codeKey)
        redis.call('del', challengeKey)
        return '5:'
    end
    return '4:' .. attempts
end

redis.call('del', codeKey)
redis.call('hset', challengeKey, 'status', verifiedStatus)
redis.call('hset', challengeKey, 'secureToken', secureToken)
redis.call('set', tokenKey, secureContextJson, 'EX', tokenTtlSeconds)
return '6:' .. secureToken
