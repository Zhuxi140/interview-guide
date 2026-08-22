package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单类资源的数据保留策略项。
 */
@Schema(description = "数据保留策略项")
public record RetentionPolicyItemVO(
        @Schema(description = "资源类型", example = "API_LOG")
        String resourceType,
        @Schema(description = "热数据保留天数", example = "30")
        Integer hotRetentionDays,
        @Schema(description = "是否启用归档")
        Boolean archiveEnabled,
        @Schema(description = "冷数据保留天数，可为空表示永久保留", example = "365")
        Integer coldRetentionDays
) {
}
