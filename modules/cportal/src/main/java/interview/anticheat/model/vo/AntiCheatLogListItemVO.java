package interview.anticheat.model.vo;

import interview.anticheat.model.enums.AntiCheatEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 平台端防作弊日志列表项。
 */
@Builder
@Schema(description = "平台端防作弊日志列表项")
public record AntiCheatLogListItemVO(
        @Schema(description = "日志 ID")
        Long id,
        @Schema(description = "涉嫌异常的候选人用户 ID")
        Long userId,
        @Schema(description = "关联的面试会话 ID")
        Long sessionId,
        @Schema(description = "事件类型", example = "PAGE_BLUR")
        AntiCheatEventType eventType,
        @Schema(description = "异常行为持续时间（毫秒）", example = "3200")
        Long durationMs,
        @Schema(description = "是否关联了证据材料")
        Boolean evidenceAvailable,
        @Schema(description = "违规发生时间")
        OffsetDateTime createdAt
) {
}
