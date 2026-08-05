package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试流程终态判定结果。
 */
@Getter
@AllArgsConstructor
public enum InterviewFlowStatusEnum {

    NO_SCHEDULE("尚未创建任何面试排期"),
    IN_PROGRESS("面试流程进行中"),
    INTERVIEW_COMPLETED("面试流程已完成"),
    REJECTED_TERMINAL("面试流程已淘汰终止");

    private final String message;
}
