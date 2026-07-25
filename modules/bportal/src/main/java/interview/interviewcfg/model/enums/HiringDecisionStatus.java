package interview.interviewcfg.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 招聘结果状态。
 */
@Getter
@AllArgsConstructor
public enum HiringDecisionStatus {

    OFFERED("已发放录用意向"),
    HIRED("已录用"),
    REJECTED("已淘汰");

    private final String message;
}
