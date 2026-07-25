package interview.textinterview.model.vo;

import interview.common.enums.InterviewSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "创建面试会话响应（含首题）")
public record InterviewSessionCreateVO(
        @Schema(description = "面试会话ID")
        Long sessionId,

        @Schema(description = "会话版本号")
        Integer sessionVersion,

        @Schema(description = "首道面试题")
        InterviewQuestionVO question,

        @Schema(description = "会话状态", example = "IN_PROGRESS")
        InterviewSessionStatus status
) {
}
