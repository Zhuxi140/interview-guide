package interview.system.auth;

import interview.system.auth.enums.SmsType;

/**
 * @author zhuxi
 * @apiNote 认证服务Redis Key
 * @since 2026/5/27 14:05
 */


public interface AuthKeyConstant {

    String PREFIX = "sys:auth";
    String SMS_CODE_PREFIX =":sms:code:";
    String SMS_LOCK_PREFIX = ":sms:lock:";

    String TOKEN_BAN_PREFIX = "token:ban:";


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
}
