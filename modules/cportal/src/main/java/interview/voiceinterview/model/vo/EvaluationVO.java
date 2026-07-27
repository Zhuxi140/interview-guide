package interview.voiceinterview.model.vo;

import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 语音评估查询响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "语音评估查询响应")
public record EvaluationVO(
        @Schema(description = "评估状态 PENDING / PROCESSING / COMPLETED / FAILED", example = "PENDING")
        VoiceEvaluationStatus evaluationStatus,

        @Schema(description = "已开始的评估尝试次数")
        Integer attemptNo,

        @Schema(description = "失败原因（仅 FAILED 时返回）")
        String failureReason,

        @Schema(description = "评估详情（仅 COMPLETED 时返回）")
        EvaluationDetailVO evaluation
) {
}
