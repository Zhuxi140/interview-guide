package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试排期生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum InterviewScheduleStatus {

    PENDING_CONFIRMATION("待候选人确认"),
    CONFIRMED("已确认"),
    IN_PROGRESS("面试进行中"),
    COMPLETED("面试已完成"),
    DECLINED("候选人已拒绝"),
    CANCELLED("排期已取消"),
    NO_SHOW("候选人未到场");

    private final String message;

    public static boolean isTerminal(InterviewScheduleStatus status){
        switch (status){
            case COMPLETED:
            case DECLINED:
            case CANCELLED:
            case NO_SHOW:
                return true;
            default:
                return false;
        }
    }

}
