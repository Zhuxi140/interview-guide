package interview.resume.event;

public record ResumeAnalysisCreatedEvent(
        Long messageId,
        Long resumeId,
        Long userId
) {
}
