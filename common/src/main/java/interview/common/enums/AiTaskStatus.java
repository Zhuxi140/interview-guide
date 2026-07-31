package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 业务任务状态。
 */
@Getter
@AllArgsConstructor
public enum AiTaskStatus {
    WAITING_PROFILE("等待人才画像"),
    PENDING("等待执行"),
    PROCESSING("执行中"),
    COMPLETED("已完成"),
    FAILED("执行失败");

    private final String message;
}
