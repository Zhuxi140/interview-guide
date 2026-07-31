package interview.resume.model.message;

/**
 * 简历 AI 分析消息载荷。
 *
 * @param resumeId 简历 ID
 * @param userId 用户 ID
 */
public record ResumeAnalysisCommand(
        Long resumeId,
        Long userId
) {
}
