package interview.voiceinterview.model.vo;

import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 结束语音会话响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "结束语音会话响应")
public record EndSessionVO(
        @Schema(description = "统一会话ID")
        Long id,

        @Schema(description = "会话状态", example = "COMPLETED")
        String status,

        @Schema(description = "实际连麦秒数")
        Integer actualDurationSeconds,

        @Schema(description = "评估状态", example = "PENDING")
        VoiceEvaluationStatus evaluationStatus,

        @Schema(description = "结束时间")
        OffsetDateTime endedAt
) {
}
