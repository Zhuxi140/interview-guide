package interview.system.auth;


import interview.system.auth.model.req.SmsSendReq;

/**
 * @author zhuxi
 * @apiNote 短信服务
 * @since 2026/5/27 14:05
 */

public interface SmsService {

    /**
     * 发送短信
     * @param smsReq 短信请求
     */
    void sendSms(SmsSendReq smsReq);
}
