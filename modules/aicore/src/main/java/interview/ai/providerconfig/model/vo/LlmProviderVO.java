package interview.ai.providerconfig.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 大模型提供商配置响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "大模型提供商配置响应")
public record LlmProviderVO(
        @Schema(description = "提供商配置 ID", example = "dashscope-chat")
        String providerId,

        @Schema(description = "API 网关地址", example = "https://dashscope.aliyuncs.com/compatible-mode")
        String baseUrl,

        @Schema(description = "固定掩码，仅表示 API Key 已配置", example = "******")
        String apiKeyMasked,

        @Schema(description = "主力对话模型名", example = "qwen-max")
        String model,

        @Schema(description = "路由开关")
        Boolean enabled,

        @Schema(description = "乐观锁版本")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt
) {
}
