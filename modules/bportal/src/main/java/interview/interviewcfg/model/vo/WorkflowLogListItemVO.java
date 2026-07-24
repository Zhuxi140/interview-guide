package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "人才流转状态机历史日志项")
public record WorkflowLogListItemVO(
        @Schema(description = "日志主键")
        Long id,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "跃迁前状态")
        String fromStatus,

        @Schema(description = "跃迁后状态")
        String toStatus,

        @Schema(description = "操作人用户ID（系统自动推进为0）")
        Long operatorUserId,

        @Schema(description = "状态流转备注")
        String transitionReason,

        @Schema(description = "状态变迁时间")
        OffsetDateTime createdAt
) {
}
