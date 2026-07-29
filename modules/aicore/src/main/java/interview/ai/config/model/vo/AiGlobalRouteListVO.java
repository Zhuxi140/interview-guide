package interview.ai.config.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 全局默认路由列表")
public record AiGlobalRouteListVO(
        @Schema(description = "按模型能力划分的默认路由")
        List<AiGlobalRouteVO> routes
) {
}
