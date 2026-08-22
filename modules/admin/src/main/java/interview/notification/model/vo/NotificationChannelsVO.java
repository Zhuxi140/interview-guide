package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 渠道配置集合（含聚合版本号）。
 */
@Schema(description = "渠道配置集合")
public record NotificationChannelsVO(
        @Schema(description = "渠道配置列表")
        List<NotificationChannelVO> channels,
        @Schema(description = "聚合版本号（各渠道行版本最大值），PUT 时作为 expectedVersion",
                example = "0")
        Integer version
) {
}
