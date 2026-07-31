package interview.matching.event;

/**
 * 候选人岗位适配预测任务创建事件。
 */
public record CandidateJobMatchCreatedEvent(
        Long messageId,
        Long applicationId
) {
}
