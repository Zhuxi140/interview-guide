
local key = KEYS[1]
local inputCode = ARGV[1]

local code = redis.call('get', key)

-- 验证码不存在
if not code then
    return 0
end

-- 验证码错误
if code ~= inputCode then
    return 1
end

redis.call('del', key)
return 2