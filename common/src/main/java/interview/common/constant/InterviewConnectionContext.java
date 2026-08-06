package interview.common.constant;

import lombok.Builder;
import lombok.Data;

/**
 * 面试连接凭证携带的业务上下文。
 *
 * @author zhuxi
 */
@Data
@Builder
public class InterviewConnectionContext {

    private Long userId;
    private Long sessionId;
    private Long scheduleId;
    private Long enterpriseId;
    private String role;
}