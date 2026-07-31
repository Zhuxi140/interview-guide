package interview.ai.llm.client;

import interview.ai.llm.model.AiResult;
import interview.common.enums.AiSceneCode;

/**
 * 统一 LLM 调用入口：业务代码只依赖本接口
 */
public interface UnifiedChatClient {

    /**
     * 结构化调用：LLM 输出直接映射为 Java 对象
     *
     * @param sceneCode    场景编码
     * @param userPrompt   用户提示词
     * @param responseType 期望返回的 Java 类型
     * @param <T>          结构化结果类型
     * @return 模型结果及非敏感配置快照
     */
    <T> AiResult<T> callStructured(
            AiSceneCode sceneCode,
            String userPrompt,
            Class<T> responseType
    );

    /**
     * 文本调用：返回纯文本
     *
     * @param sceneCode  场景编码
     * @param userPrompt 用户提示词
     * @return 模型文本及非敏感配置快照
     */
    AiResult<String> callText(AiSceneCode sceneCode, String userPrompt);
}
