package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 一次性安全令牌允许执行的业务动作。
 *
 * @author zhuxi
 */
@Getter
@AllArgsConstructor
public enum SecureActionType {

    UPDATE_ENTERPRISE_EMAIL("更新企业联系邮箱"),
    UPDATE_ENTERPRISE_PHONE("更新企业联系电话"),
    DELETE_ENTERPRISE("注销企业"),
    REVOKE_DEVICE("下线登录设备");

    private final String description;
}
