package interview.compliance.model.enums;

/**
 * @author zhuxi
 * @apiNote 敏感词触发动作（sensitive_words.action_type，VARCHAR）
 */
public enum SensitiveAction {

    /**
     * 拦截
     */
    BLOCK,

    /**
     * 替换
     */
    REPLACE,

    /**
     * 告警
     */
    ALERT
}
