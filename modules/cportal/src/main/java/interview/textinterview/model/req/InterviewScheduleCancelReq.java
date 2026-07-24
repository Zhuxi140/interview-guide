package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "候选人取消已确认面试请求")
public record InterviewScheduleCancelReq(
        @NotBlank(message = "取消原因不能为空")
        @Schema(description = "取消原因", example = "时间冲突")
        String reason,

        @Schema(description = "期望版本号", example = "1")
        Integer expectedVersion
) {
}
