package interview.textinterview.model.vo;

import interview.common.enums.InterviewSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "候选人就绪响应")
public record InterviewSessionReadyVO(
        @Schema(description = "会话 ID", example = "10001")
        Long sessionId,

        @Schema(description = "会话状态", example = "IN_PROGRESS")
        InterviewSessionStatus status,

        @Schema(description = "首题是否正在生成中", example = "true")
        Boolean firstQuestionPending
) {
}
