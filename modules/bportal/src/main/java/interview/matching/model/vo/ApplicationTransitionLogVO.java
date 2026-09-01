package interview.matching.model.vo;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "投递状态流转记录")
public record ApplicationTransitionLogVO(
        @Schema(description = "流转记录 ID")
        Long id,

        @Schema(description = "流转前状态")
        JobApplicationStatus fromStatus,

        @Schema(description = "流转后状态")
        JobApplicationStatus toStatus,

        @Schema(description = "操作用户 ID；系统自动推进时为 0")
        Long operatorUserId,

        @Schema(description = "流转原因")
        String transitionReason,

        @Schema(description = "流转时间")
        OffsetDateTime createdAt
) {
}
