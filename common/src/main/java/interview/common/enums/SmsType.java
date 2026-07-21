package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 短信类型枚举
 */


@AllArgsConstructor
@Getter
public enum SmsType {
    REGISTER("REGISTER", "注册"),
    LOGIN("LOGIN", "登录"),
    RESET_PWD("RESET_PWD", "忘记密码"),
    BIND_PHONE("BIND_PHONE", "绑定手机"),
    RISK_VERIFY("RISK_VERIFY", "风控验证"),
    SECURE_CHALLENGE("SECURE_CHALLENGE", "安全操作验证"),
    ENTERPRISE_VERIFY_OLD("ENTERPRISE_VERIFY_OLD","双重手机验证第一重"),
    ENTERPRISE_VERIFY_NEW("ENTERPRISE_VERIFY_NEW","双重手机验证第二重");


    private final String code;
    private final String msg;

    /**
     * 根据code获取枚举
     * @param code code
     * @return SmsType
     */
    public static SmsType getByCode(String code) {
        for (SmsType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("无效的短信类型");
    }
}
