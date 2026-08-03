package interview.textinterview.model.req;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "候选人确认或拒绝面试邀请请求")
public record InterviewDecisionReq(
        @NotNull(message = "期望状态不能为空")
        @Schema(description = "客户端读取到的排期状态", example = "PENDING_CONFIRMATION")
        InterviewScheduleStatus expectedStatus,

        @Size(max = 256, message = "原因长度不能超过256个字符")
        @Schema(description = "拒绝原因；确认邀请时无需传入", example = "时间无法协调")
        String reason,

        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "期望版本号", example = "1")
        Integer expectedVersion
) {
}
