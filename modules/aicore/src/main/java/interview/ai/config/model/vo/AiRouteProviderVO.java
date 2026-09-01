package interview.ai.config.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 路由候选 Provider")
public record AiRouteProviderVO(
        @Schema(description = "Provider ID") String providerId,
        @Schema(description = "故障转移优先级，数值越小越优先") Integer priority,
        @Schema(description = "加权路由权重") Integer weight
) {
}
