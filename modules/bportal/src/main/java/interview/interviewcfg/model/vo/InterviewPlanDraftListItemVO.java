package interview.interviewcfg.model.vo;

import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.common.enums.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Agent 面试编排草案列表项")
public record InterviewPlanDraftListItemVO(
        @Schema(description = "草案ID", example = "32001")
        Long draftId,

        @Schema(description = "投递ID", example = "21001")
        Long applicationId,

        @Schema(description = "模板ID", example = "31001")
        Long templateId,

        @Schema(description = "面试类型 TEXT / VOICE / CODE")
        InterviewType interviewType,

        @Schema(description = "草案状态")
        InterviewPlanDraftStatus status,

        @Schema(description = "失败原因（仅 status=FAILED 时有值）", nullable = true)
        String failureReason,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
