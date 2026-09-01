package interview.textinterview.event;

import interview.common.enums.InterviewType;
import interview.common.enums.QuestionKind;

/**
 * 面试会话就绪领域事件（触发异步出题）
 */
public record InterviewSessionReadyEvent(
        Long sessionId,
        Long scheduleId,
        Long enterpriseId,
        InterviewType sessionType,
        QuestionKind questionKind,
        String traceId
) {
}
