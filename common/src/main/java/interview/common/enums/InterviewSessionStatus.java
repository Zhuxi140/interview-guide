package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试运行时会话状态。
 */
@Getter
@AllArgsConstructor
public enum InterviewSessionStatus {

    CREATED("已创建"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    TERMINATED("已终止");

    private final String message;
}
