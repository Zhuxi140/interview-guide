package interview.ai.providerconfig.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote LLM 全局设置响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "LLM 全局设置响应")
public record LlmSettingVO(
        @Schema(description = "全局单例 ID", example = "1")
        String id,

        @Schema(description = "默认对话提供商 ID", example = "dashscope-chat")
        String defaultChatProviderId,

        @Schema(description = "默认 Embedding 提供商 ID", example = "openai-embedding")
        String defaultEmbeddingProviderId,

        @Schema(description = "乐观锁版本")
        Integer version,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt
) {
}
