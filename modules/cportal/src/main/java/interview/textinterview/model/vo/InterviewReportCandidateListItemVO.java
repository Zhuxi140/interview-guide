package interview.textinterview.model.vo;

import interview.common.enums.InterviewReportGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "C端面试报告列表项")
public record InterviewReportCandidateListItemVO(
        @Schema(description = "报告ID")
        Long id,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "企业名称")
        String enterpriseName,

        @Schema(description = "岗位名称")
        String jobTitle,

        @Schema(description = "报告生成状态 PENDING / PROCESSING / COMPLETED / FAILED")
        InterviewReportGenerationStatus generationStatus,

        @Schema(description = "系统最终打分 0-100（未完成时为 null）")
        Integer overallAiScore,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
