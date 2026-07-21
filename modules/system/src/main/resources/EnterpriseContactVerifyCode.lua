-- 原子核销企业联系电话验证码、推进状态并按需签发安全令牌。
local flowKey = KEYS[1]
local codeKey = KEYS[2]
local secureTokenKey = KEYS[3]

local userId = ARGV[1]
local enterpriseId = ARGV[2]
local expectedStage = ARGV[3]
local nextStage = ARGV[4]
local inputCode = ARGV[5]
local secureToken = ARGV[6]
local secureContextJson = ARGV[7]
local secureTokenTtl = tonumber(ARGV[8])

-- 验证流程必须存在且属于当前用户和目标企业。
if redis.call('exists', flowKey) == 0 then
    return '0:'
end

-- 校验 flow 中的 userId 和 enterpriseId 是否与传入参数一致，防止越权操作
if redis.call('hget', flowKey, 'userId') ~= userId
        or redis.call('hget', flowKey, 'enterpriseId') ~= enterpriseId then
    return '1:'
end

-- 已推进到目标状态时返回原令牌，其他非预期状态拒绝处理。
local stage = redis.call('hget', flowKey, 'stage')
if stage == nextStage then
    local existingToken = redis.call('hget', flowKey, 'secureToken') or ''
    return '6:' .. existingToken
end
if stage ~= expectedStage then
    return '2:'
end

-- 校验验证码；成功后立即删除并推进流程状态。
local code = redis.call('get', codeKey)
if not code then
    return '3:'
end
if code ~= inputCode then
    return '4:'
end

redis.call('del', codeKey)
redis.call('hset', flowKey, 'stage', nextStage)

-- 新号码验证成功时，同时保存安全上下文并缩短流程有效期。
if secureToken ~= '' then
    redis.call('hset', flowKey, 'secureToken', secureToken)
    redis.call('set', secureTokenKey, secureContextJson, 'EX', secureTokenTtl)
    redis.call('expire', flowKey, secureTokenTtl)
end

return '5:' .. secureToken
