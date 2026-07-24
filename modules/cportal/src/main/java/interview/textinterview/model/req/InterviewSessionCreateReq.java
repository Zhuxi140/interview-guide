package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "候选人创建文本面试会话请求")
public record InterviewSessionCreateReq(
        @NotNull(message = "排期ID不能为空")
        @Schema(description = "面试排期ID", example = "1001")
        Long scheduleId
) {
}
