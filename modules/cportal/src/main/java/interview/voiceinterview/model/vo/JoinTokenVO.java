package interview.voiceinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 加入语音面试响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "加入语音面试响应")
public record JoinTokenVO(
        @Schema(description = "统一会话ID")
        Long sessionId,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "会话类型", example = "VOICE")
        String sessionType,

        @Schema(description = "当前尝试次数")
        Short attemptNo,

        @Schema(description = "会话状态", example = "CREATED")
        String status,

        @Schema(description = "WebSocket 连接凭证")
        String connectionToken,

        @Schema(description = "信令服务器地址", example = "wss://voice.example.com/ws")
        String signalingUrl,

        @Schema(description = "凭证有效秒数", example = "60")
        Integer expiresInSeconds,

        @Schema(description = "会话最后事件序号")
        Long lastEventSequence
) {
}
