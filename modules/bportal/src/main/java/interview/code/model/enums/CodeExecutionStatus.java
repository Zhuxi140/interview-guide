package interview.code.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 代码提交执行状态。
 */
@Getter
@AllArgsConstructor
public enum CodeExecutionStatus {

    /** 已入队，等待沙箱执行 */
    QUEUED("已入队"),

    /** 沙箱执行中 */
    RUNNING("执行中"),

    /** 全部用例通过 */
    PASS("通过"),

    /** 存在未通过用例 */
    FAIL("未通过"),

    /** 执行超时 */
    TIMEOUT("执行超时"),

    /** 执行异常（编译失败、沙箱错误等） */
    ERROR("执行异常");

    private final String message;
}
