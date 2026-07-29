package interview.ai.config.model.vo;

import interview.common.enums.AiModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Schema(description = "AI 场景执行参数响应")
public record LlmSceneVO(

        @Schema(description = "场景编码", example = "RESUME_ANALYSIS")
        String sceneCode,

        @Schema(description = "模型类型", example = "CHAT")
        AiModelType modelType,

        @Schema(description = "绑定的 Provider ID，为空时使用全局默认路由")
        String providerId,

        @Schema(description = "采样温度")
        BigDecimal temperature,

        @Schema(description = "核采样参数")
        BigDecimal topP,

        @Schema(description = "输入上下文预算", example = "128000")
        Integer maxInputTokens,

        @Schema(description = "最大输出 Token 数", example = "4096")
        Integer maxOutputTokens,

        @Schema(description = "单次模型调用超时（秒）", example = "60")
        Integer timeoutSeconds,

        @Schema(description = "提示词模板版本", example = "resume-analysis-v1")
        String promptVersion,

        @Schema(description = "供应商特有参数 JSON")
        String extraOptions,

        @Schema(description = "场景开关")
        Boolean enabled,

        @Schema(description = "乐观锁版本")
        Integer version,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt

) {
}
