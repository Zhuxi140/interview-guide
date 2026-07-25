package interview.interviewcfg.model.req;

import interview.interviewcfg.model.enums.HiringDecisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "HR 流转面试最终状态请求")
public record InterviewScheduleStatusReq(
        @NotNull(message = "目标状态不能为空")
        @Schema(description = "目标状态 OFFERED / HIRED / REJECTED", example = "HIRED")
        HiringDecisionStatus status,

        @Schema(description = "流转备注")
        String transitionReason,

        @Schema(description = "录用详情快照（JSON）")
        String offerDetail,

        @NotNull(message = "乐观锁版本号不能为空")
        @Schema(description = "期望版本号", example = "1")
        Integer expectedVersion
) {
}
