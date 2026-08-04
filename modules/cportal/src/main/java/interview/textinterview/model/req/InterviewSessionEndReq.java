package interview.textinterview.model.req;

import interview.common.enums.InterviewSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "结束面试会话请求")
public record InterviewSessionEndReq(
        @NotNull(message = "期望会话状态不能为空")
        @Schema(description = "期望会话状态", example = "IN_PROGRESS")
        InterviewSessionStatus expectedStatus
) {
}
