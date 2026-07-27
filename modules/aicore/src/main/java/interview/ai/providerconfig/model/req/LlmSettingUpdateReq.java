package interview.ai.providerconfig.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 更新 LLM 全局设置请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "更新 LLM 全局设置请求")
public class LlmSettingUpdateReq {

    @NotNull(message = "乐观锁版本不能为空")
    @Schema(description = "期望版本号（CAS 乐观锁）", example = "0")
    private Integer expectedVersion;

    @NotBlank(message = "默认对话提供商不能为空")
    @Schema(description = "默认对话提供商 ID", example = "dashscope-chat")
    private String defaultChatProviderId;

    @Schema(description = "默认 Embedding 提供商 ID", example = "openai-embedding")
    private String defaultEmbeddingProviderId;
}
