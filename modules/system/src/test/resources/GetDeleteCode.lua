local stored = redis.call('GET', KEYS[1])
if stored == nil then
    return 0
end
if stored == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 2
else
    return 1
end
