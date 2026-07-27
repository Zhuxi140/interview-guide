package interview.ai.providerconfig.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 大模型连接测试响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "大模型连接测试响应")
public record LlmProviderTestConnectionVO(
        @Schema(description = "提供商配置 ID", example = "dashscope-chat")
        String providerId,

        @Schema(description = "测试的模型名", example = "qwen-max")
        String model,

        @Schema(description = "是否可达")
        Boolean reachable,

        @Schema(description = "响应延迟（毫秒）")
        Long latencyMs,

        @Schema(description = "检查时间")
        OffsetDateTime checkedAt
) {
}
