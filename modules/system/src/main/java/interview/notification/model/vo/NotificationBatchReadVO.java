package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量标记已读结果。
 */
@Schema(description = "批量标记已读结果")
public record NotificationBatchReadVO(
        @Schema(description = "本次标记已读的消息数量", example = "5")
        Integer updatedCount
) {
}
