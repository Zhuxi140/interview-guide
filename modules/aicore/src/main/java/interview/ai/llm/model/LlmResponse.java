package interview.ai.llm.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * LLM响应结果泛型类
 *
 * @param <T> 响应数据泛型类型
 */
@AllArgsConstructor
@Data
public class LlmResponse<T> {
    /**
     * 响应内容主体
     */
    private T content;

    /**
     * 输入token数
     */
    private long inputTokens;

    /**
     * 输出token数
     */
    private long outputTokens;

    /**
     * 总token数
     */
    private long totalTokens;

    /**
     * 调用延迟毫秒数
     */
    private long latencyMs;
}
