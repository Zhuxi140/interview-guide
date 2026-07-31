package interview.api.aicore.dto;

import interview.common.enums.AiModelType;
import interview.common.enums.AiSceneCode;
import interview.common.enums.RouteSource;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;


@Builder
public record LlmConfigSnapshotDTO(
        Integer schemaVersion,
        AiSceneCode sceneCode,
        AiModelType modelType,
        RouteSource routeSource,
        String providerId,
        String baseUrl,
        String model,
        LlmParametersSnapshot parameters,
        PromptSnapshot prompt,
        ConfigVersions configVersions,
        OffsetDateTime resolvedAt
){


    /**
     * 场景执行参数快照。
     */
    @Builder
    public record LlmParametersSnapshot(
            BigDecimal temperature,
            BigDecimal topP,
            Integer maxInputTokens,
            Integer maxOutputTokens,
            Integer timeoutSeconds,
            Map<String, Object> extraOptions
    ) {
    }

    /**
     * 提示词版本及内容摘要。
     */
    public record PromptSnapshot(
            String version,
            String sha256
    ) {
    }

    /**
     * 路由所读取的配置版本。
     */
    public record ConfigVersions(
            Integer sceneConfigVersion,
            Integer providerVersion,
            Integer globalRouteVersion
    ) {
    }
}

