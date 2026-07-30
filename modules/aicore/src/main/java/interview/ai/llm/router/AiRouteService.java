package interview.ai.llm.router;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.entity.LlmSceneConfig;
import interview.common.enums.AiSceneCode;
import interview.ai.config.service.AiGlobalRouteService;
import interview.ai.config.service.LlmProviderConfigService;
import interview.ai.config.service.LlmSceneConfigService;
import interview.ai.llm.model.RouteResult;
import interview.common.enums.AiModelType;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.security.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRouteService {

    private final LlmSceneConfigService llmSceneConfigService;
    private final LlmProviderConfigService llmProviderConfigService;
    private final AiGlobalRouteService  aiGlobalRouteService;
    private final SecretCipher secretCipher;

    @Cacheable(value = "routeInfo", key = "#sceneCode")
    public RouteResult route(AiSceneCode sceneCode){
        LlmSceneConfig sceneConfig = llmSceneConfigService.lambdaQuery()
                .select(LlmSceneConfig::getModelType,
                        LlmSceneConfig::getProviderId,
                        LlmSceneConfig::getTemperature,
                        LlmSceneConfig::getTopP,
                        LlmSceneConfig::getMaxInputTokens,
                        LlmSceneConfig::getMaxOutputTokens,
                        LlmSceneConfig::getTimeoutSeconds,
                        LlmSceneConfig::getPromptVersion,
                        LlmSceneConfig::getExtraOptions,
                        LlmSceneConfig::getVersion
                        )
                .eq(LlmSceneConfig::getSceneCode, sceneCode)
                .eq(LlmSceneConfig::getEnabled, true)
                .one();

        LlmProviderConfig providerInfo = null;
        if (sceneConfig == null) {
            log.error("AI场景配置异常—— sceneConfig = null, sceneCode: {}", sceneCode);
            throw new BusinessException(ErrorCode.AI_SCENE_NOT_FOUND);
        }
        String providerId = sceneConfig.getProviderId();
        AiModelType modelType = sceneConfig.getModelType();
        Integer globalRouterVersion = null;
        if (StrUtil.isNotBlank(providerId)){
            providerInfo = getProviderInfo(providerId,sceneCode);
        }else {
            AiGlobalRoute globalRoute = aiGlobalRouteService.lambdaQuery()
                    .select(AiGlobalRoute::getProviderId, AiGlobalRoute::getVersion)
                    .eq(AiGlobalRoute::getModelType, modelType)
                    .one();

            if (StrUtil.isNotBlank(globalRoute.getProviderId())) {
                String globalRouteProviderId = globalRoute.getProviderId();
                providerInfo = getProviderInfo(globalRouteProviderId,sceneCode);
                providerId = globalRouteProviderId;
                globalRouterVersion = globalRoute.getVersion();
            }else{
                log.error("AI全局默认路由异常—— AiGlobalRoute = null, sceneCode: {}", sceneCode);
                throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
            }
        }

        if (!providerInfo.getModelType().equals(modelType)){
            throw new BusinessException(ErrorCode.AI_PROVIDER_TYPE_MISMATCH);
        }

        String apiKey = secretCipher.decrypt(providerInfo.getApiKeyCiphertext());
        return RouteResult.builder()
                .sceneCode(sceneCode)
                .modelType(sceneConfig.getModelType())
                .providerId(providerId)
                .baseUrl(providerInfo.getBaseUrl())
                .model(providerInfo.getModel())
                .apiKey(apiKey)
                .temperature(sceneConfig.getTemperature())
                .topP(sceneConfig.getTopP())
                .maxInputTokens(sceneConfig.getMaxInputTokens())
                .maxOutputTokens(sceneConfig.getMaxOutputTokens())
                .timeoutSeconds(sceneConfig.getTimeoutSeconds())
                .promptVersion(sceneConfig.getPromptVersion())
                .extraOptions(parseExtraOptions(sceneConfig.getExtraOptions()))
                .sceneConfigVersion(sceneConfig.getVersion())
                .providerVersion(providerInfo.getVersion())
                .globalRouteVersion(globalRouterVersion)
                .build();
    }

    /**
     * 将 JSON 字符串转换为 Map
     */
    private Map<String, Object> parseExtraOptions(String extraOptions) {
        if (StrUtil.isBlank(extraOptions)) {
            return Map.of();
        }
        return Map.copyOf(JSONUtil.toBean(extraOptions, Map.class));
    }

    private LlmProviderConfig getProviderInfo(String providerId,AiSceneCode sceneCode){
        LlmProviderConfig providerInfo = llmProviderConfigService.lambdaQuery()
                .select(
                        LlmProviderConfig::getModelType,
                        LlmProviderConfig::getBaseUrl,
                        LlmProviderConfig::getModel,
                        LlmProviderConfig::getApiKeyCiphertext,
                        LlmProviderConfig::getVersion,
                        LlmProviderConfig::getEnabled
                )
                .eq(LlmProviderConfig::getId, providerId)
                .one();

        if (providerInfo == null){
            log.error("AI供应商配置异常—— 全局路由表和场景表中的providerId均无获取有效供应商信息, sceneCode: {}, providerId: {}", sceneCode, providerId);
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }

        if (providerInfo.getEnabled().equals(Boolean.FALSE)){
            log.error("AI供应商配置异常—— 供应商已禁用, sceneCode: {}, providerId: {}", sceneCode, providerId);
            throw new BusinessException(ErrorCode.AI_PROVIDER_DISABLED);
        }
        return providerInfo;
    }
}
