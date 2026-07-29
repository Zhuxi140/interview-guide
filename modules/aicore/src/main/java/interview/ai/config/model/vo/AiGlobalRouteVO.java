package interview.ai.config.model.vo;

import interview.common.enums.AiModelType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "AI 全局默认路由")
public record AiGlobalRouteVO(
        @Schema(description = "模型能力类型", example = "CHAT")
        AiModelType modelType,

        @Schema(description = "默认 Provider ID；为空表示尚未配置", example = "dashscope-chat")
        String providerId,

        @Schema(description = "乐观锁版本")
        Integer version,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt
) {
}
