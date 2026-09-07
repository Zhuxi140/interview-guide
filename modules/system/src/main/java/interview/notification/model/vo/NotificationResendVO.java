package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知重发结果。
 *
 * @param id         通知（发送记录）ID
 * @param sendStatus 重发后的发送状态；占位阶段同步完成，直接返回最终状态
 */
@Schema(description = "通知重发结果")
public record NotificationResendVO(
        @Schema(description = "通知 ID")
        Long id,
        @Schema(description = "发送状态", example = "SENT")
        String sendStatus
) {
}
