package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "AI 推荐排期请求")
public record AiSuggestionReq(
        @NotNull(message = "投递ID不能为空")
        @Schema(description = "投递记录ID", example = "1001")
        Long applicationId,

        @Schema(description = "期望面试官用户ID（为空则推荐任意可用面试官）")
        Long interviewerUserId,

        @NotNull(message = "面试时长不能为空")
        @Schema(description = "面试时长（分钟）", example = "60")
        Integer durationMinutes
) {
}
