package interview.textinterview.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "候选人接受/拒绝面试响应")
public record InterviewDecisionVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "排期状态 CONFIRMED / DECLINED")
        InterviewScheduleStatus status,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "提示信息", example = "已确认参加面试")
        String message
) {
}
