package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 我的消息列表项。
 * 注：sys_notifications 未设计跳转目标列（targetType/targetId），待后续扩列后再补齐该字段。
 */
@Schema(description = "我的消息列表项")
public record NotificationListItemVO(
        @Schema(description = "消息 ID")
        Long id,
        @Schema(description = "通知类型", example = "INTERVIEW")
        String notifyType,
        @Schema(description = "消息标题")
        String title,
        @Schema(description = "消息正文")
        String content,
        @Schema(description = "是否已读")
        Boolean isRead,
        @Schema(description = "发送时间")
        OffsetDateTime createdAt
) {
}
