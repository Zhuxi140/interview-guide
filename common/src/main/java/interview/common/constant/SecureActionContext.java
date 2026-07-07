package interview.common.constant;

import interview.common.enums.SmsType;
import lombok.Builder;
import lombok.Data;

/**
 * @author zhuxi
 */
@Data
@Builder
public class SecureActionContext {
    private Long userId;
    private SmsType actionType;
    private String targetPhone;
}
