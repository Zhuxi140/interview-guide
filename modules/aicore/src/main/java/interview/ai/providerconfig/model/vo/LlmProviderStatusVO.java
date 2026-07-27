package interview.ai.providerconfig.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 大模型提供商启停响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "大模型提供商启停响应")
public record LlmProviderStatusVO(
        @Schema(description = "提供商配置 ID", example = "dashscope-chat")
        String providerId,

        @Schema(description = "路由开关")
        Boolean enabled,

        @Schema(description = "乐观锁版本")
        Integer version,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt
) {
}
