package interview.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 短信类型枚举
 * @since 2026/5/27 14:05
 */


@AllArgsConstructor
@Getter
public enum SmsType {
    REGISTER("REGISTER", "注册"),
    LOGIN("LOGIN", "登录"),
    RESET_PWD("RESET_PWD", "忘记密码"),
    BIND_PHONE("BIND_PHONE", "绑定手机");

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
