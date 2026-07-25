package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试报告生成状态。
 */
@Getter
@AllArgsConstructor
public enum InterviewReportGenerationStatus {

    PENDING("等待生成"),
    PROCESSING("生成中"),
    COMPLETED("生成完成"),
    FAILED("生成失败");

    private final String message;
}
