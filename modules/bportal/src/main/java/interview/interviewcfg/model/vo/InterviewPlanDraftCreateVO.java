package interview.interviewcfg.model.vo;

import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agent 面试编排草案受理结果")
public record InterviewPlanDraftCreateVO(
        @Schema(description = "草案ID", example = "32001")
        Long draftId,

        @Schema(description = "投递ID", example = "21001")
        Long applicationId,

        @Schema(description = "草案状态", example = "PENDING")
        InterviewPlanDraftStatus status,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version
) {
}
