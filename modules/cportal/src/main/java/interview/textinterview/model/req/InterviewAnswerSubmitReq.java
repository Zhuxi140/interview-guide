package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "候选人提交当前题答案请求")
public record InterviewAnswerSubmitReq(
        @NotNull(message = "题目ID不能为空")
        @Schema(description = "当前题目ID", example = "5001")
        Long questionId,

        @NotNull(message = "期望题目索引不能为空")
        @Schema(description = "期望当前题目索引（乐观锁）", example = "2")
        Integer expectedQuestionIndex,

        @NotNull(message = "期望会话版本号不能为空")
        @Schema(description = "期望会话版本号（乐观锁）", example = "1")
        Integer expectedSessionVersion,

        @NotBlank(message = "答案不能为空")
        @Schema(description = "求职者提交的文本答案", example = "Spring Boot 的核心特点是...")
        String userAnswer
) {
}
