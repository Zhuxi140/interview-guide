package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "结束面试会话响应")
public record InterviewSessionEndVO(
        @Schema(description = "会话ID")
        Long sessionId,

        @Schema(description = "会话状态", example = "COMPLETED")
        String status,

        @Schema(description = "报告生成状态 PENDING / PROCESSING / COMPLETED / FAILED", example = "PENDING")
        String reportStatus,

        @Schema(description = "会话版本号")
        Integer sessionVersion,

        @Schema(description = "提示信息", example = "面试已结束，报告生成中")
        String message
) {
}
