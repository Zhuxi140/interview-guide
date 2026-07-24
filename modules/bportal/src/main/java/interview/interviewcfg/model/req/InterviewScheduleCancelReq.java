package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "取消面试排期请求")
public record InterviewScheduleCancelReq(
        @NotBlank(message = "取消原因不能为空")
        @Schema(description = "取消原因", example = "候选人放弃面试")
        String reason,

        @NotNull(message = "乐观锁版本号不能为空")
        @Schema(description = "期望版本号", example = "1")
        Integer expectedVersion
) {
}
