package interview.ai.config.model.vo;

import interview.common.enums.AiModelType;
import interview.common.enums.AiRouteStrategy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "AI 路由详情")
public record AiRouteDetailVO(
        @Schema(description = "模型能力类型") AiModelType modelType,
        @Schema(description = "路由策略") AiRouteStrategy strategy,
        @Schema(description = "候选 Provider") List<AiRouteProviderVO> providers,
        @Schema(description = "配置版本") Integer version,
        @Schema(description = "更新时间") OffsetDateTime updatedAt
) {
}
