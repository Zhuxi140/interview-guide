package interview.system.tenant.model.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 企业状态枚举
 */

@Getter
@AllArgsConstructor
public enum EnterpriseStatus {

    PENDING(2, "PENDING"),
    NORMAL(1, "NORMAL"),
    PAUSED(0, "PAUSED"),
    CANCELLED(-1, "CANCELLED");
    @EnumValue
    private final int code;
    private final String message;

    /**
     * 根据状态码获取状态
     * @param code 状态码
     * @return 状态
     */
    public static EnterpriseStatus of(int code) {
        for (EnterpriseStatus enterpriseStatus : EnterpriseStatus.values()) {
            if (enterpriseStatus.getCode() == code) {
                return enterpriseStatus;
            }
        }
        throw new IllegalArgumentException("Invalid EnterpriseStatus code: " + code);
    }
}
