package interview.voiceinterview.model.vo;

import interview.voiceinterview.model.enums.VoiceInterviewPhase;
import interview.voiceinterview.model.enums.VoiceMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 语音消息明细
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "语音消息明细")
public record VoiceMessageVO(
        @Schema(description = "消息ID")
        Long id,

        @Schema(description = "事件幂等ID")
        String eventId,

        @Schema(description = "消息类型 USER_SPEECH / AI_SPEECH / SYSTEM", example = "USER_SPEECH")
        VoiceMessageType messageType,

        @Schema(description = "当前面试阶段", example = "TECH")
        VoiceInterviewPhase currentPhase,

        @Schema(description = "ASR 转写文本")
        String asrText,

        @Schema(description = "AI 回复文本")
        String llmResponseText,

        @Schema(description = "事件序号")
        Long sequenceNum,

        @Schema(description = "事件时间")
        OffsetDateTime createdAt,

        @Schema(description = "ASR/LLM/TTS 调用链ID")
        String traceId
) {
}
