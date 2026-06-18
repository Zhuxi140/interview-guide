package interview.system.auth.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 用户状态枚举
 */

@AllArgsConstructor
@Getter
public enum UserStatus {

    NORMAL(1, "正常"),
    DISABLED(0, "禁用");

    @EnumValue
    private final Integer code;
    private final String msg;


    /**
     * 根据code获取msg
     * @param code code
     * @return msg
     */
    public static String getMsg(Integer code) {
        for (UserStatus value : values()) {
            if (value.code.equals(code)) {
                return value.msg;
            }
        }
        throw new IllegalArgumentException("无法识别的用户状态" + code);
    }
}
