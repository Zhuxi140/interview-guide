package interview.interviewcfg.model.vo;

import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Agent 面试编排草案详情")
@Builder
public record InterviewPlanDraftDetailVO(
        @Schema(description = "草案ID", example = "32001")
        Long draftId,

        @Schema(description = "投递ID", example = "21001")
        Long applicationId,

        @Schema(description = "草案状态")
        InterviewPlanDraftStatus status,

        @Schema(description = "失败原因（仅 status=FAILED 时有值）", nullable = true)
        String failureReason,

        @Schema(description = "草案计划内容（仅 status=READY/APPLIED 时有值）", nullable = true)
        PlanVO plan,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
    @Schema(description = "草案计划详情")
    public record PlanVO(
            @Schema(description = "阶段计划")
            List<StagePlanVO> stages,

            @Schema(description = "排期建议")
            List<ScheduleSuggestionVO> scheduleSuggestions
    ) {
    }

    @Schema(description = "单阶段计划")
    public record StagePlanVO(
            @Schema(description = "阶段编码", example = "TECHNICAL")
            String phaseCode,

            @Schema(description = "考察目标")
            String objectives,

            @Schema(description = "题纲")
            String questionOutline,

            @Schema(description = "建议时长（分钟）", example = "60")
            Integer durationMinutes
    ) {
    }

    @Schema(description = "单条排期建议")
    public record ScheduleSuggestionVO(
            @Schema(description = "建议ID", example = "1")
            Long suggestionId,

            @Schema(description = "建议面试官ID", example = "5001")
            Long interviewerUserId,

            @Schema(description = "建议面试时间")
            OffsetDateTime interviewTime,

            @Schema(description = "推荐理由")
            String reason
    ) {
    }
}