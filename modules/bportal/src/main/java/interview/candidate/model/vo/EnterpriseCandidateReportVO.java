package interview.candidate.model.vo;

import interview.common.enums.InterviewReportGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "候选人面试报告摘要")
public record EnterpriseCandidateReportVO(
        @Schema(description = "报告 ID") Long reportId,
        @Schema(description = "排期 ID") Long scheduleId,
        @Schema(description = "生成状态") InterviewReportGenerationStatus status,
        @Schema(description = "AI 总分") Integer overallAiScore,
        @Schema(description = "完成时间") OffsetDateTime completedAt
) {
}
