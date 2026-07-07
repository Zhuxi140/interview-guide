package interview.common.constant;

import interview.common.enums.SmsType;

/**
 * @author zhuxi
 * @apiNote 认证服务Redis Key
 */


public interface AuthKeyConstant {

    String PREFIX = "sys:auth";
    String SMS_CODE_PREFIX =":sms:code:";
    String SMS_LOCK_PREFIX = ":sms:lock:";
    String SMS_SENSITIVE_ACTION_TOKEN_PREFIX = ":sms:secure:token:";

    String TOKEN_BAN_PREFIX = "token:ban:";


    /**
     * 获取敏感操作令牌key（仅以 token 为维度，不包含 phone/smsType，方便 SecureAspect 查找）
     * @param token 令牌
     * @return 敏感操作令牌key
     */
    static String getSmsSensitiveActionTokenKey(String token) {
        return PREFIX + SMS_SENSITIVE_ACTION_TOKEN_PREFIX + token;
    }


    /**
     * 获取短信验证码key
     * @param phone 手机号
     * @param type 短信类型
     * @return 短信验证码key
     */
     static String getSmsCodeKey(String phone, SmsType type){
        return PREFIX + SMS_CODE_PREFIX + phone + ":" + type.name();
    }

    /**
     * 获取短信验证码锁key
     * @param phone 手机号
     * @param type 短信类型
     * @return 短信验证码锁key
     */
    static String getSmsLockKey(String phone, SmsType type){
        return PREFIX + SMS_LOCK_PREFIX + phone + ":" + type.name();
    }

    /**
     * 获取token禁用key
     * @param token token
     * @return token禁用key
     */
    static String getTokenBanKey(String token){
        return PREFIX + TOKEN_BAN_PREFIX + token;
    }
}
