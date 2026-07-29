package interview.ai.config.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "更新 AI 场景执行参数请求")
public class LlmSceneUpdateReq {

    @NotNull(message = "乐观锁版本不能为空")
    @Schema(description = "期望版本号（CAS 乐观锁）", example = "0")
    private Integer expectedVersion;

    @Schema(description = "绑定的 Provider ID，传 null 表示使用全局默认路由")
    private String providerId;

    @Schema(description = "采样温度，传 null 表示由模型适配器采用代码默认值")
    private BigDecimal temperature;

    @Schema(description = "核采样参数，传 null 表示由模型适配器采用代码默认值")
    private BigDecimal topP;

    @NotNull(message = "输入上下文预算不能为空")
    @Schema(description = "输入上下文预算", example = "128000")
    private Integer maxInputTokens;

    @NotNull(message = "最大输出 Token 数不能为空")
    @Schema(description = "最大输出 Token 数", example = "4096")
    private Integer maxOutputTokens;

    @NotNull(message = "单次模型调用超时不能为空")
    @Schema(description = "单次模型调用超时（秒）", example = "60")
    private Integer timeoutSeconds;

    @NotBlank(message = "提示词模板版本不能为空")
    @Schema(description = "提示词模板版本", example = "resume-analysis-v1")
    private String promptVersion;

    @NotNull(message = "供应商特有参数不能为空，无参数请传 {}")
    @Schema(description = "供应商特有参数 JSON")
    private String extraOptions;
}
