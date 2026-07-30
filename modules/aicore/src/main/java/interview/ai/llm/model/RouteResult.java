package interview.ai.llm.model;

import interview.common.enums.AiSceneCode;
import interview.common.enums.AiModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 模型路由结果
 */
@Getter
@AllArgsConstructor
@Builder
public class RouteResult {

    /**
     * 场景编码
     */
    private final AiSceneCode sceneCode;

    /**
     * 模型类型
     */
    private final AiModelType modelType;

    /**
     * 供应商ID
     */
    private final String providerId;

/*    private final AiProviderProtocol protocolType;*/

    /**
     * 基础URL
     */
    private final String baseUrl;

    /**
     * 模型名称
     */
    private final String model;

    /**
     * API密钥（仅内存使用，禁止日志输出）
     */
    @ToString.Exclude
    private final String apiKey;

    /**
     * 温度参数
     */
    private final BigDecimal temperature;

    /**
     * Top-P参数
     */
    private final BigDecimal topP;

    /**
     * 最大输入token数
     */
    private final Integer maxInputTokens;

    /**
     * 最大输出token数
     */
    private final Integer maxOutputTokens;

    /**
     * 超时秒数
     */
    private final Integer timeoutSeconds;

    /**
     * 提示词版本
     */
    private final String promptVersion;

    /**
     * 场景配置版本
     */
    private final Integer sceneConfigVersion;

    /**
     * 供应商配置版本
     */
    private final Integer providerVersion;

    /**
     * 全局路由配置版本
     */
    private final Integer globalRouteVersion;

    /**
     * 扩展配置
     */
    private final Map<String, Object> extraOptions;
}