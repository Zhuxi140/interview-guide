package interview.ai.llm.model;

import interview.common.enums.AiModelType;
import interview.common.enums.AiSceneCode;
import interview.common.enums.RouteSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 单次 LLM 调用实际使用的非敏感配置快照。
 */
public record LlmConfigSnapshot(
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
) {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    /**
     * 从运行时路由结果生成不包含 API Key 的持久化快照。
     *
     * @param route        本次调用的路由结果
     * @param systemPrompt 本次调用的完整系统提示词
     * @return 非敏感配置快照
     */
    public static LlmConfigSnapshot from(RouteResult route, String systemPrompt) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(systemPrompt, "systemPrompt");

        // 仅显式复制可审计配置，禁止直接序列化包含明文 API Key 的 RouteResult。
        Map<String, Object> extraOptions = route.getExtraOptions() == null
                ? Map.of()
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(route.getExtraOptions())
                );
        RouteSource routeSource = route.getGlobalRouteVersion() == null
                ? RouteSource.SCENE
                : RouteSource.GLOBAL;

        return new LlmConfigSnapshot(
                CURRENT_SCHEMA_VERSION,
                route.getSceneCode(),
                route.getModelType(),
                routeSource,
                route.getProviderId(),
                route.getBaseUrl(),
                route.getModel(),
                new LlmParametersSnapshot(
                        route.getTemperature(),
                        route.getTopP(),
                        route.getMaxInputTokens(),
                        route.getMaxOutputTokens(),
                        route.getTimeoutSeconds(),
                        extraOptions
                ),
                new PromptSnapshot(
                        route.getPromptVersion(),
                        sha256(systemPrompt)
                ),
                new ConfigVersions(
                        route.getSceneConfigVersion(),
                        route.getProviderVersion(),
                        route.getGlobalRouteVersion()
                ),
                OffsetDateTime.now()
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK does not support SHA-256", e);
        }
    }

    /**
     * 场景执行参数快照。
     */
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
