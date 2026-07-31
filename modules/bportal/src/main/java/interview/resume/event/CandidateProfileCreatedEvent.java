package interview.resume.event;

/**
 * 人才画像任务创建事件。
 */
public record CandidateProfileCreatedEvent(
        Long messageId,
        Long resumeId,
        Long candidateId
) {
}
