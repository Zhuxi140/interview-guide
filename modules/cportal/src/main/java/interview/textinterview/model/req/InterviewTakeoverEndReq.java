package interview.textinterview.model.req;

import interview.textinterview.model.enums.InterviewTakeoverEndAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "结束人工接管请求")
public record InterviewTakeoverEndReq(
        @NotNull(message = "结束接管后的动作不能为空")
        @Schema(description = "结束接管后的会话动作", example = "RESUME_AI")
        InterviewTakeoverEndAction action
) {
}
