package interview.ai.llm.model;

import interview.common.enums.AiSceneCode;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * LLM请求参数
 */
@AllArgsConstructor
@Data
public class LlmRequest {
    /**
     * 场景编码，标识不同的AI调用场景
     */
    private AiSceneCode sceneCode;

    /**
     * 系统提示词，定义AI的角色和行为约束
     */
    private String systemPrompt;

    /**
     * 用户提示词，包含具体的任务内容和输入数据
     */
    private String userPrompt;

    /**
     * 温度参数，控制输出的随机性，0-2之间，值越大越随机
     */
    private Float temperature;

    /**
     * 最大生成token数，限制输出长度
     */
    private Integer maxTokens;

    /**
     * Top-p采样参数，核采样概率阈值，0-1之间
     */
    private Float topP;
}
