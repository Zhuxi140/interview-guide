package interview.system.auth.service;


import interview.common.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;

/**
 * @author zhuxi
 * @apiNote 短信服务
 */

public interface SmsService {

    /**
     * 发送公开场景短信验证码
     * @param smsReq 短信请求
     */
    void sendSms(SmsSendReq smsReq);

    /**
     * 向指定 Redis key 写入业务验证码
     * @param phone 手机号
     * @param type 验证码类型
     * @param codeKey 验证码 Redis key
     */
    void sendCode(String phone, SmsType type, String codeKey);

    /**
     * 原子校验并核销验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 验证码类型
     */
    void verifyCode(String phone, String code, SmsType type);

}
