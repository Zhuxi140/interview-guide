package interview.compliance.model.vo;

import interview.compliance.model.enums.PolicyAction;
import interview.compliance.model.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 创建/更新用户 API 策略结果。
 */
@Builder
@Schema(description = "创建/更新用户 API 策略结果")
public record ApiPolicyMutationVO(
        @Schema(description = "策略 ID")
        Long id,
        @Schema(description = "被管控用户 ID")
        Long userId,
        @Schema(description = "策略类型", example = "RATE_LIMIT")
        PolicyType policyType,
        @Schema(description = "动作", example = "BLOCK")
        PolicyAction actionType,
        @Schema(description = "并发版本号；创建为 0，更新为期望版本号 +1", example = "0")
        Integer version
) {
}
