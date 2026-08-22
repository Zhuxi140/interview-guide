package interview.compliance.model.enums;

/**
 * @author zhuxi
 * @apiNote 用户 API 策略类型（user_api_policies.policy_type，VARCHAR）
 */
public enum PolicyType {

    /**
     * 限流：必须携带正整数 limitCount/limitSeconds
     */
    RATE_LIMIT,

    /**
     * 风控拦截名单：不得携带限流参数
     */
    BLACKLIST,

    /**
     * 白名单：仅跳过限流与附加风控，不得携带限流参数
     */
    WHITELIST;

    /**
     * 判断是否为名单类策略（黑名单/白名单）
     * @return true 表示 BLACKLIST 或 WHITELIST
     */
    public boolean isListType() {
        return this == BLACKLIST || this == WHITELIST;
    }

    /**
     * 获取互斥的另一类名单策略
     * @return BLACKLIST 与 WHITELIST 互为对方；RATE_LIMIT 无互斥项返回 null
     */
    public PolicyType oppositeListType() {
        if (this == BLACKLIST) {
            return WHITELIST;
        }
        if (this == WHITELIST) {
            return BLACKLIST;
        }
        return null;
    }
}
