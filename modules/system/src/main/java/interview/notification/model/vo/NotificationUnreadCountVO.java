package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 未读消息数量。
 */
@Schema(description = "未读消息数量")
public record NotificationUnreadCountVO(
        @Schema(description = "未读消息数量", example = "3")
        Long unreadCount
) {
}
