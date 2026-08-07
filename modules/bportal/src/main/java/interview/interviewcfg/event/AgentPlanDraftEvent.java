package interview.interviewcfg.event;

/**
 * Agent 面试编排草案生成任务已受理事件。
 *
 * @param messageId 生成消息 ID
 * @param draftId 草案 ID
 * @param enterpriseId 企业 ID
 * @param applicationId 投递 ID
 */
public record AgentPlanDraftEvent(
        Long messageId,
        Long draftId,
        Long enterpriseId,
        Long applicationId
) {
}