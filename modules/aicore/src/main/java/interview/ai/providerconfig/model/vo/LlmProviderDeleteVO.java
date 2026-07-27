package interview.ai.providerconfig.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author zhuxi
 * @apiNote 大模型提供商删除响应
 * @since 2026/7/26 14:05
 */
@Schema(description = "大模型提供商删除响应")
public record LlmProviderDeleteVO(
        @Schema(description = "已删除的提供商配置 ID", example = "dashscope-chat")
        String providerId
) {
}
