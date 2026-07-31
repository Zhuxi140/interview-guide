package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.api.aicore.dto.AiDimensionMatch;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ScreeningRecommendation;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * HR AI 初筛结果响应。
 */
@Schema(description = "HR AI 初筛结果响应")
public record ApplicationAiScreeningVO(
        @Schema(description = "初筛任务 ID")
        Long screeningId,
        @Schema(description = "投递 ID")
        Long applicationId,
        @Schema(description = "人才画像 ID")
        Long candidateProfileId,
        @Schema(description = "任务状态")
        AiTaskStatus status,
        @Schema(description = "总体匹配分")
        Integer overallMatchScore,
        @Schema(description = "岗位维度匹配结果")
        List<AiDimensionMatch> dimensionMatches,
        @Schema(description = "AI 初筛建议")
        ScreeningRecommendation recommendation,
        @Schema(description = "HR 最终审核决定")
        JobApplicationStatus reviewDecision,
        @Schema(description = "审核人 ID")
        Long reviewedBy,
        @Schema(description = "审核时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime reviewedAt,
        @Schema(description = "失败原因摘要")
        String failureReason,
        @Schema(description = "任务创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
