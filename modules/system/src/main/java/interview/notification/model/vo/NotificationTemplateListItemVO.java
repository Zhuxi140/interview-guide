package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 通知模板列表项。
 */
@Schema(description = "通知模板列表项")
public record NotificationTemplateListItemVO(
        @Schema(description = "模板 ID")
        Long id,
        @Schema(description = "业务场景", example = "INTERVIEW_INVITE")
        String notifyScene,
        @Schema(description = "发送渠道", example = "EMAIL")
        String channelType,
        @Schema(description = "标题模板")
        String title,
        @Schema(description = "正文模板")
        String contentTemplate,
        @Schema(description = "是否启用")
        Boolean enabled,
        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
