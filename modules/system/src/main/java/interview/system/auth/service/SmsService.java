package interview.system.auth.service;


import interview.common.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.auth.model.req.VerifyReq;

/**
 * @author zhuxi
 * @apiNote 短信服务
 */

public interface SmsService {

    /**
     * 发送短信
     * @param smsReq 短信请求
     */
    void sendSms(SmsSendReq smsReq);

    /**
     * 验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 验证码类型
     */
    void verifyCode(String phone, String code, SmsType type);


    /**
     * 效验敏感操作
     * @param verifyReq 验证请求
     * @return 敏感操作令牌token
     */
    String verifyForSensitiveAction(VerifyReq verifyReq);
}
