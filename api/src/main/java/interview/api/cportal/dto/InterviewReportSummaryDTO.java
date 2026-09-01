package interview.api.cportal.dto;

import interview.common.enums.InterviewReportGenerationStatus;

import java.time.OffsetDateTime;

/**
 * 跨模块使用的面试报告摘要。
 */
public record InterviewReportSummaryDTO(
        Long reportId,
        Long scheduleId,
        InterviewReportGenerationStatus status,
        Integer overallAiScore,
        OffsetDateTime completedAt
) {
}
