package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "结束面试会话请求")
public record InterviewSessionEndReq(
        @NotNull(message = "期望会话版本号不能为空")
        @Schema(description = "期望会话版本号（乐观锁）", example = "5")
        Integer expectedSessionVersion
) {
}
