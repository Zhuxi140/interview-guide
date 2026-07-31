package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.common.enums.AiTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 候选人私有岗位适配预测响应。
 */
@Schema(description = "候选人私有岗位适配预测响应")
public record CandidateJobMatchAnalysisVO(
        @Schema(description = "预测任务 ID")
        Long analysisId,
        @Schema(description = "投递 ID")
        Long applicationId,
        @Schema(description = "任务状态")
        AiTaskStatus status,
        @Schema(description = "岗位适配分")
        Integer matchScore,
        @Schema(description = "初筛通过率预测")
        Integer passProbability,
        @Schema(description = "候选人优势")
        List<String> strengths,
        @Schema(description = "与岗位要求的差距")
        List<String> gaps,
        @Schema(description = "失败原因摘要")
        String failureReason,
        @Schema(description = "分析完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime analyzedAt
) {
}
