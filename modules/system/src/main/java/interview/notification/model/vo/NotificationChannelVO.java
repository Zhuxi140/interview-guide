package interview.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 渠道配置展示项（查询脱敏，仅返回 configSummary）。
 */
@Schema(description = "渠道配置展示项")
public record NotificationChannelVO(
        @Schema(description = "渠道类型", example = "EMAIL")
        String channelType,
        @Schema(description = "渠道启用开关")
        Boolean enabled,
        @Schema(description = "供应商标识", example = "ALIYUN_SMS")
        String provider,
        @Schema(description = "渠道参数脱敏摘要，凭证字段固定为 ******",
                example = "{\"host\":\"smtp.example.com\",\"password\":\"******\"}")
        String configSummary,
        @Schema(description = "行级乐观锁版本号", example = "0")
        Integer version
) {
}
