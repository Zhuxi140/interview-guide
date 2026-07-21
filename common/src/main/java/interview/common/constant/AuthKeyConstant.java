package interview.common.constant;

import interview.common.enums.SmsType;

/**
 * @author zhuxi
 * @apiNote 认证服务Redis Key
 */


public interface AuthKeyConstant {

    String PREFIX = "sys:auth";
    String SMS_CODE_PREFIX = ":sms:code:";
    String SMS_LOCK_PREFIX = ":sms:lock:";
    String SECURE_ACTION_TOKEN_PREFIX = ":secure:token:";
    String SECURE_CHALLENGE_PREFIX = ":secure:challenge:";
    String ENTERPRISE_CONTACT_FLOW_PREFIX = ":enterprise:contact:flow:";
    String ENTERPRISE_CONTACT_CODE_PREFIX = ":enterprise:contact:code:";

    // 企业联系电话验证流程 Hash 字段
    String ENTERPRISE_CONTACT_FLOW_FIELD_USER_ID = "userId";
    String ENTERPRISE_CONTACT_FLOW_FIELD_ENTERPRISE_ID = "enterpriseId";
    String ENTERPRISE_CONTACT_FLOW_FIELD_OLD_PHONE = "oldPhone";
    String ENTERPRISE_CONTACT_FLOW_FIELD_NEW_PHONE = "newPhone";
    String ENTERPRISE_CONTACT_FLOW_FIELD_STAGE = "stage";
    String ENTERPRISE_CONTACT_FLOW_FIELD_SECURE_TOKEN = "secureToken";

    // 通用安全验证 Challenge Hash 字段
    String SECURE_CHALLENGE_FIELD_USER_ID = "userId";
    String SECURE_CHALLENGE_FIELD_ACTION_TYPE = "actionType";
    String SECURE_CHALLENGE_FIELD_RESOURCE_ID = "resourceId";
    String SECURE_CHALLENGE_FIELD_VERIFY_PHONE = "verifyPhone";
    String SECURE_CHALLENGE_FIELD_STATUS = "status";
    String SECURE_CHALLENGE_FIELD_FAILED_ATTEMPTS = "failedAttempts";
    String SECURE_CHALLENGE_FIELD_SECURE_TOKEN = "secureToken";

    String TOKEN_BAN_PREFIX = "token:ban:";


    /**
     * 获取一次性安全操作令牌 key
     * @param token 令牌
     * @return 安全操作令牌 key
     */
    static String getSecureActionTokenKey(String token) {
        int separator = token.indexOf('.');
        if (separator > 0) {
            String challengeId = token.substring(0, separator);
            return PREFIX + SECURE_ACTION_TOKEN_PREFIX
                    + "{" + challengeId + "}:" + token;
        }
        return PREFIX + SECURE_ACTION_TOKEN_PREFIX + token;
    }

    /**
     * 获取安全验证 Challenge 上下文 key
     * @param challengeId Challenge ID
     * @return Challenge 上下文 key
     */
    static String getSecureChallengeKey(String challengeId) {
        return PREFIX + SECURE_CHALLENGE_PREFIX
                + "{" + challengeId + "}:context";
    }

    /**
     * 获取安全验证 Challenge 验证码 key
     * @param challengeId Challenge ID
     * @return Challenge 验证码 key
     */
    static String getSecureChallengeCodeKey(String challengeId) {
        return PREFIX + SECURE_CHALLENGE_PREFIX
                + "{" + challengeId + "}:code";
    }

    /**
     * 获取企业联系电话验证流程 key
     * @param flowId 验证流程 ID
     * @return 验证流程 key
     */
    static String getEnterpriseContactFlowKey(String flowId) {
        return PREFIX + ENTERPRISE_CONTACT_FLOW_PREFIX
                + "{" + flowId + "}:flow";
    }

    /**
     * 获取企业联系电话验证码 key
     * @param flowId 验证流程 ID
     * @param type 验证码类型
     * @return 验证码 key
     */
    static String getEnterpriseContactCodeKey(String flowId, SmsType type) {
        return PREFIX + ENTERPRISE_CONTACT_CODE_PREFIX
                + "{" + flowId + "}:" + type.name();
    }


    /**
     * 获取短信验证码key
     * @param phone 手机号
     * @param type 短信类型
     * @return 短信验证码key
     */
    static String getSmsCodeKey(String phone, SmsType type) {
        return PREFIX + SMS_CODE_PREFIX + phone + ":" + type.name();
    }

    /**
     * 获取短信验证码锁key
     * @param phone 手机号
     * @param type 短信类型
     * @return 短信验证码锁key
     */
    static String getSmsLockKey(String phone, SmsType type) {
        return PREFIX + SMS_LOCK_PREFIX + phone + ":" + type.name();
    }

    /**
     * 获取token禁用key
     * @param token token
     * @return token禁用key
     */
    static String getTokenBanKey(String token) {
        return PREFIX + TOKEN_BAN_PREFIX + token;
    }
}
