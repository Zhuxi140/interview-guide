package interview.common.constant;

import interview.common.enums.SecureActionType;
import lombok.Builder;
import lombok.Data;

/**
 * 一次性安全操作令牌携带的业务上下文。
 *
 * @author zhuxi
 */
@Data
@Builder
public class SecureActionContext {

    public static final String REQUEST_ATTRIBUTE = "SECURE_ACTION_CONTEXT";

    private Long userId;
    private SecureActionType actionType;
    private Long resourceId;
    private String targetPhone;
    private Long enterpriseId;
    private String sourcePhone;
    private String challengeId;
}
