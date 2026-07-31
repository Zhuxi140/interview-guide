package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * HR 审核 AI 初筛建议响应。
 */
@Schema(description = "HR 审核 AI 初筛建议响应")
public record ApplicationAiScreeningReviewVO(
        @Schema(description = "初筛任务 ID")
        Long screeningId,
        @Schema(description = "投递 ID")
        Long applicationId,
        @Schema(description = "最终投递状态")
        JobApplicationStatus applicationStatus,
        @Schema(description = "审核时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime reviewedAt
) {
}
