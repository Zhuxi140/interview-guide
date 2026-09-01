package interview.ai.config.model.vo;

import interview.common.enums.AiModelType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 路由运行健康状态")
public record AiRouteHealthVO(
        @Schema(description = "模型能力类型") AiModelType modelType,
        @Schema(description = "候选 Provider 健康状态") List<AiRouteProviderHealthVO> providers
) {
}
