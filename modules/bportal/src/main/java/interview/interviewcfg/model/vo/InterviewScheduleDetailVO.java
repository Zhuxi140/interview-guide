package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "面试排期详情")
public record InterviewScheduleDetailVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "投递记录ID")
        Long applicationId,

        @Schema(description = "候选人用户ID")
        Long candidateUserId,

        @Schema(description = "岗位ID")
        Long jobId,

        @Schema(description = "模板ID")
        Long templateId,

        @Schema(description = "面试官用户ID")
        Long interviewerUserId,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "面试类型 TEXT / VOICE / CODE")
        String interviewType,

        @Schema(description = "排期状态")
        String status,

        @Schema(description = "录用详情快照（JSON）")
        String offerDetail,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
