package interview.interviewcfg.model.bo;

/**
 * 已领取的 Agent 草案生成执行上下文。
 *
 * @param draftId 草案 ID
 * @param messageId 生成消息 ID，同时作为任务 ID
 * @param enterpriseId 企业 ID
 * @param applicationId 投递 ID
 * @param templateId 模板 ID
 * @param requestJson HR 编排参数快照
 * @param inputSnapshotJson 岗位/候选人/模板阶段配置快照
 * @param attemptCount 当前执行次数
 */
public record InterviewPlanGenerationExecutionBO(
        Long draftId,
        Long messageId,
        Long enterpriseId,
        Long applicationId,
        Long templateId,
        String requestJson,
        String inputSnapshotJson,
        Integer attemptCount
) {
}