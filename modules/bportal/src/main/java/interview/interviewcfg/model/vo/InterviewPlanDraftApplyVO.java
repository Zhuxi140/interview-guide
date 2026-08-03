package interview.interviewcfg.model.vo;

import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Agent 面试编排草案应用结果")
public record InterviewPlanDraftApplyVO(
        @Schema(description = "草案ID", example = "32001")
        Long draftId,

        @Schema(description = "草案状态", example = "APPLIED")
        InterviewPlanDraftStatus status,

        @Schema(description = "由草案创建的排期ID")
        List<Long> scheduleIds,

        @Schema(description = "草案应用时间")
        OffsetDateTime appliedAt
) {
}
