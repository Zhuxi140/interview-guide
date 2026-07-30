package interview.resume.model.entity;

import interview.common.enums.AiSceneCode;
import interview.common.enums.AiModelType;
import java.math.BigDecimal;
import java.util.Map;

public record AiConfigSnapshot(
        AiSceneCode sceneCode,
        Integer sceneConfigVersion,
        AiModelType modelType,
        String providerId,
        Integer providerConfigVersion,
        Integer globalRouteVersion,
        String baseUrl,
        String model,
        BigDecimal temperature,
        BigDecimal topP,
        Integer maxInputTokens,
        Integer maxOutputTokens,
        Integer timeoutSeconds,
        String promptVersion,
        Map<String, Object> extraOptions
){
}
