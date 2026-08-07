package interview.api.aicore;

import interview.api.aicore.dto.AiInterviewPlanInput;
import interview.api.aicore.dto.AiInterviewPlanResult;

/**
 * 面试编排跨度 Agent 的 AI 内部接口。
 *
 * <p>当前仅留存契约，由后续阶段在 aicore 模块实现；尚未注册 Bean 前，
 * 调用方只负责在消息链路上执信，不直接依赖 AI 具体实现。</p>
 */
public interface InterviewPlanTradeAiApi {

    /**
     * 生成面试编排草案计划。
     *
     * @param input AI 编排输入
     * @return AI 编排结果；阶段集合与顺序必须以 inputSnapshotJson 中的模板阶段为准
     */
    AiInterviewPlanResult generateInterviewPlan(AiInterviewPlanInput input);
}