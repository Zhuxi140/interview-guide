package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "面试报告详情")
public record InterviewReportDetailVO(
        @Schema(description = "报告唯一主键")
        Long id,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "系统最终打分 0-100")
        Integer overallAiScore,

        @Schema(description = "AI 录用评估综述意见")
        String executiveSummary,

        @Schema(description = "沟通逻辑维度得分 0-100")
        Integer communicationScore,

        @Schema(description = "代码沙箱表现专家点评")
        String codeCapabilityReview,

        @Schema(description = "报告生成时间")
        OffsetDateTime createdAt
) {
}
