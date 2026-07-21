-- 原子绑定企业新联系电话并推进验证状态。
local flowKey = KEYS[1]
local userId = ARGV[1]
local enterpriseId = ARGV[2]
local newPhone = ARGV[3]
local oldVerified = ARGV[4]
local waitNewVerify = ARGV[5]

-- 验证流程必须存在。
if redis.call('exists', flowKey) == 0 then
    return '0:'
end

-- 流程必须属于当前用户和目标企业。
if redis.call('hget', flowKey, 'userId') ~= userId
        or redis.call('hget', flowKey, 'enterpriseId') ~= enterpriseId then
    return '1:'
end

-- 首次绑定时保存新号码并推进状态；相同号码重试保持幂等。
local stage = redis.call('hget', flowKey, 'stage')
if stage == oldVerified then
    redis.call('hset', flowKey, 'newPhone', newPhone)
    redis.call('hset', flowKey, 'stage', waitNewVerify)
    return '5:'
end

if stage == waitNewVerify
        and redis.call('hget', flowKey, 'newPhone') == newPhone then
    return '6:'
end

return '2:'
