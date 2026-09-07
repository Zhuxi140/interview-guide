package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 全站通知发送记录列表项。
 */
@Schema(description = "全站通知发送记录列表项")
public record NotificationSendRecordListItemVO(
        @Schema(description = "记录 ID")
        Long id,
        @Schema(description = "接收用户 ID")
        Long userId,
        @Schema(description = "业务场景", example = "INTERVIEW_INVITE")
        String notifyScene,
        @Schema(description = "发送渠道", example = "EMAIL")
        String channelType,
        @Schema(description = "发送状态", example = "SENT")
        String sendStatus,
        @Schema(description = "发送失败原因")
        String failureReason,
        @Schema(description = "发送时间")
        OffsetDateTime createdAt
) {
}
