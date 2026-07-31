package interview.matching.event;

/**
 * HR AI 初筛任务创建事件。
 */
public record ApplicationScreeningCreatedEvent(
        Long messageId,
        Long applicationId
) {
}
