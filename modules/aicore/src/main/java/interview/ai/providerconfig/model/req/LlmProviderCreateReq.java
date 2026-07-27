package interview.ai.providerconfig.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 创建大模型提供商配置请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "创建大模型提供商配置请求")
public class LlmProviderCreateReq {

    @NotBlank(message = "提供商配置ID不能为空")
    @Schema(description = "提供商配置 ID", example = "dashscope-chat")
    private String providerId;

    @NotBlank(message = "API 网关地址不能为空")
    @Schema(description = "API 网关地址", example = "https://dashscope.aliyuncs.com/compatible-mode")
    private String baseUrl;

    @NotBlank(message = "API Key 不能为空")
    @Schema(description = "API Key（明文，服务端加密存储）")
    private String apiKey;

    @NotBlank(message = "模型名不能为空")
    @Schema(description = "主力对话模型名", example = "qwen-max")
    private String model;

    @Schema(description = "路由开关，默认关闭", example = "false")
    private Boolean enabled = false;
}
