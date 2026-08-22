package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 编辑通知模板结果。
 */
@Schema(description = "编辑通知模板结果")
public record NotificationTemplateUpdateVO(
        @Schema(description = "模板 ID")
        Long id,
        @Schema(description = "更新后的版本号", example = "1")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
