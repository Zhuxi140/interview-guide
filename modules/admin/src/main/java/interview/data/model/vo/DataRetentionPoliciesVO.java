package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 数据保留策略配置。
 */
@Schema(description = "数据保留策略配置")
public record DataRetentionPoliciesVO(
        @Schema(description = "聚合版本号，PUT 时作为 expectedVersion", example = "0")
        Integer version,
        @Schema(description = "各类资源的保留策略列表")
        List<RetentionPolicyItemVO> policies,
        @Schema(description = "最近更新时间")
        OffsetDateTime updatedAt
) {
}
