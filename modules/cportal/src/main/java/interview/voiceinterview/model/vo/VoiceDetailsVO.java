package interview.voiceinterview.model.vo;

import interview.voiceinterview.model.enums.VoiceInterviewPhase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 语音会话详情
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "语音会话详情")
public record VoiceDetailsVO(
        @Schema(description = "当前面试阶段", example = "INTRO")
        VoiceInterviewPhase currentPhase,

        @Schema(description = "实际连麦秒数")
        Integer actualDurationSeconds
) {
}
