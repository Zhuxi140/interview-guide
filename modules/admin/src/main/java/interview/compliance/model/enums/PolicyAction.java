package interview.compliance.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 用户 API 策略动作（user_api_policies.action_type，SMALLINT：1 放行 / -1 拦截）
 */
@Getter
@AllArgsConstructor
public enum PolicyAction {

    /**
     * 强制放行：仅跳过限流与附加风控，不能绕过 JWT/RBAC/KYC 等安全校验
     */
    ALLOW(1, "强制放行"),

    /**
     * 强制拦截
     */
    BLOCK(-1, "强制拦截");

    @EnumValue
    private final int code;
    private final String message;
}
