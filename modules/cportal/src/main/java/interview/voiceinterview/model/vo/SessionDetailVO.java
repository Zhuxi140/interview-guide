package interview.voiceinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 会话详情响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "会话详情响应")
public record SessionDetailVO(
        @Schema(description = "会话ID")
        Long id,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "会话类型", example = "VOICE")
        String sessionType,

        @Schema(description = "当前尝试次数")
        Short attemptNo,

        @Schema(description = "会话状态", example = "IN_PROGRESS")
        String status,

        @Schema(description = "会话最后事件序号")
        Long lastEventSequence,

        @Schema(description = "语音专属信息")
        VoiceDetailsVO voiceDetails,

        @Schema(description = "开始时间")
        OffsetDateTime startedAt,

        @Schema(description = "结束时间")
        OffsetDateTime endedAt
) {
}
