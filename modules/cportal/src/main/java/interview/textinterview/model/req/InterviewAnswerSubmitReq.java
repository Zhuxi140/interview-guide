package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 作答提交请求（U3-08）。
 *
 * <p>题目信息（questionIndex / questionText / parentMessageId / followUpDepth）
 * 全部由服务端从会话时间线定位，客户端不得上报，防止伪造题目。
 * 幂等键不在此载荷内：REST 走 {@code Idempotency-Key} 请求头，
 * WS 走 {@code answer.submit} 载荷字段，由协议层解析后传入 Service。</p>
 *
 * @param userAnswer 作答正文
 */
@Schema(description = "作答提交请求")
public record InterviewAnswerSubmitReq(
        @Schema(description = "作答正文", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "作答内容不能为空")
        String userAnswer
) {
}
