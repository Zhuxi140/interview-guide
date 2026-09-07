package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 创建通知模板结果。
 */
@Schema(description = "创建通知模板结果")
public record NotificationTemplateCreateVO(
        @Schema(description = "模板 ID")
        Long id,
        @Schema(description = "业务场景", example = "INTERVIEW_INVITE")
        String notifyScene,
        @Schema(description = "发送渠道", example = "EMAIL")
        String channelType,
        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
