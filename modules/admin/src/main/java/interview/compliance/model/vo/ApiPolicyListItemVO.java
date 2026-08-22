package interview.compliance.model.vo;

import interview.compliance.model.enums.PolicyAction;
import interview.compliance.model.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 用户 API 策略列表项。
 */
@Builder
@Schema(description = "用户 API 策略列表项")
public record ApiPolicyListItemVO(
        @Schema(description = "策略 ID")
        Long id,
        @Schema(description = "被管控用户 ID")
        Long userId,
        @Schema(description = "策略类型", example = "BLACKLIST")
        PolicyType policyType,
        @Schema(description = "动作", example = "BLOCK")
        PolicyAction actionType,
        @Schema(description = "策略过期时间；为空表示永久", nullable = true)
        OffsetDateTime expireTime,
        @Schema(description = "风控原因备注", example = "接口刷量监控触发")
        String reason,
        @Schema(description = "并发版本号", example = "0")
        Integer version
) {
}
