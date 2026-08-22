package interview.compliance.model.enums;

/**
 * @author zhuxi
 * @apiNote 敏感词类别（sensitive_words.category，VARCHAR）
 */
public enum SensitiveCategory {

    /**
     * 政治敏感
     */
    POLITICAL,

    /**
     * 低俗辱骂
     */
    PROFANITY,

    /**
     * 作弊违规
     */
    CHEAT
}
