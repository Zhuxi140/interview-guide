package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 作答提交请求（U3-08）。
 *
 * <p>题目信息（questionIndex / questionText / parentMessageId / followUpDepth）
 * 全部由服务端从会话时间线定位，客户端不得上报，防止伪造题目。</p>
 *
 * @param userAnswer     作答正文
 * @param idempotencyKey 幂等键（弱网重试/WS 重放场景必传；REST 手动重试也建议传）
 */
@Schema(description = "作答提交请求")
public record InterviewAnswerSubmitReq(
        @Schema(description = "作答正文", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "作答内容不能为空")
        String userAnswer,

        @Schema(description = "幂等键（客户端生成，如 UUID）")
        String idempotencyKey
) {
}
