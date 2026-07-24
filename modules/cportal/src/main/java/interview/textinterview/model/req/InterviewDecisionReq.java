package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "候选人接受或拒绝面试邀请请求")
public record InterviewDecisionReq(
        @NotNull(message = "操作类型不能为空")
        @Schema(description = "操作 ACCEPT / DECLINE", example = "ACCEPT")
        String action,

        @NotNull(message = "乐观锁版本号不能为空")
        @Schema(description = "期望版本号", example = "1")
        Integer expectedVersion
) {
}
