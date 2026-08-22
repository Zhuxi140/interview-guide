package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 单条消息标记已读结果。
 */
@Schema(description = "单条消息标记已读结果")
public record NotificationReadVO(
        @Schema(description = "消息 ID")
        Long id,
        @Schema(description = "是否已读")
        Boolean isRead,
        @Schema(description = "已读时间（表内未单列 read_at，以更新时间代替）")
        OffsetDateTime readAt
) {
}
