package interview.framework.redis;

import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 通用 Redis Lua 脚本常量。
 *
 * @author zhuxi
 */
public final class RedisScripts {

    private RedisScripts() {
    }

    /**
     * 原子比较并删除一次性令牌（如安全操作令牌、面试连接凭证）。
     * 语义：仅当 Redis 中当前值等于期望值时才删除并返回 1，否则返回 0。
     * KEYS[1] = 令牌 key；ARGV[1] = 期望值。
     */
    public static final DefaultRedisScript<Long> CONSUME_ONCE;

    static {
        CONSUME_ONCE = new DefaultRedisScript<>();
        CONSUME_ONCE.setScriptText("""
                local value = redis.call('get', KEYS[1])
                if not value or value ~= ARGV[1] then
                    return 0
                end
                redis.call('del', KEYS[1])
                return 1
                """);
        CONSUME_ONCE.setResultType(Long.class);
    }
}