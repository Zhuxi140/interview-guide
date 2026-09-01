package interview.ai.config.model.vo;

import interview.common.enums.AiCircuitState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "AI Provider 运行健康状态")
public record AiRouteProviderHealthVO(
        @Schema(description = "Provider ID") String providerId,
        @Schema(description = "熔断器状态") AiCircuitState state,
        @Schema(description = "连续失败次数") Integer consecutiveFailures,
        @Schema(description = "最近成功时间") OffsetDateTime lastSuccessAt,
        @Schema(description = "最近失败时间") OffsetDateTime lastFailureAt
) {
}
