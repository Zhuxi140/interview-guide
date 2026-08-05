package interview.matching.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 */

@Getter
@AllArgsConstructor
public enum JobApplicationStatus {

APPLIED("已投递"),
    REVIEWING("审核中"),
    PASSED("筛选通过"),
    INTERVIEWING("面试中"),
    OFFERED("已发放录用意向"),
    HIRED("已录用"),
    REJECTED("淘汰"),
    WITHDRAWN("候选人已撤回");

    private final String msg;

}
