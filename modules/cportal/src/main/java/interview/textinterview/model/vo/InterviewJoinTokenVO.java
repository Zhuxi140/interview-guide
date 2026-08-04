package interview.textinterview.model.vo;

import interview.common.enums.InterviewSessionStatus;
import interview.common.enums.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "加入面试会话响应")
public record InterviewJoinTokenVO(
        @Schema(description = "会话ID")
        Long sessionId,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "会话类型", example = "TEXT")
        InterviewType sessionType,

        @Schema(description = "当前尝试次数", example = "1")
        Short attemptNo,

        @Schema(description = "会话状态", example = "CREATED")
        InterviewSessionStatus status,

        @Schema(description = "一次性 WebSocket 连接凭证")
        String connectionToken,

        @Schema(description = "统一 WebSocket 地址", example = "/ws/v1/interview-sessions/37001")
        String wsUrl,

        @Schema(description = "凭证有效秒数", example = "60")
        Integer expiresInSeconds,

        @Schema(description = "会话最后事件序号", example = "0")
        Long lastEventSequence
) {
}
