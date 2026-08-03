package interview.textinterview.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "候选人接受/拒绝面试响应")
public record InterviewDecisionVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "排期状态 CONFIRMED / DECLINED")
        InterviewScheduleStatus status,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
