package interview.api.aicore.dto;

/**
 * Agent 面试编排生成输入。
 *
 * @param enterpriseId 企业ID
 * @param applicationId 投递ID
 * @param requestJson HR 编排参数快照（模板、类型、候选人面试官、补充提示词等）
 * @param inputSnapshotJson 岗位/候选人/模板阶段配置快照，Agent 生成结果的阶段集合与顺序必须与之一致
 */
public record AiInterviewPlanInput(
        Long enterpriseId,
        Long applicationId,
        String requestJson,
        String inputSnapshotJson
) {
}