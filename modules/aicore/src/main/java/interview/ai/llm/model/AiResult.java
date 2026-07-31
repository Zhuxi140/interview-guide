package interview.ai.llm.model;

/**
 * LLM 调用结果及本次调用使用的配置快照。
 *
 * @param data              模型调用结果
 * @param llmConfigSnapshot 非敏感配置快照
 * @param <T>               结果类型
 */

public record AiResult<T>(
        T data,
        LlmConfigSnapshot llmConfigSnapshot
) {
}
