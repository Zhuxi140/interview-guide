package interview.ai.config.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 更新大模型提供商配置请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "更新大模型提供商配置请求")
public class LlmProviderUpdateReq {

    @NotNull(message = "乐观锁版本不能为空")
    @Schema(description = "期望版本号（CAS 乐观锁）", example = "0")
    private Integer expectedVersion;

    @Schema(description = "API 网关地址（不传则不更新）", example = "https://dashscope.aliyuncs.com/compatible-mode")
    private String baseUrl;

    @Schema(description = "API Key 明文（不传则保留原密钥）")
    private String apiKey;

    @Schema(description = "供应商模型名（不传则不更新）", example = "qwen-max")
    private String model;
}
