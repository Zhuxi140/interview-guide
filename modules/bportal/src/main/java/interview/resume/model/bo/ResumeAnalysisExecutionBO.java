package interview.resume.model.bo;

/**
 * 已领取的简历分析执行上下文。
 *
 * @param messageId 消息 ID，同时作为任务 ID
 * @param resumeId 简历 ID
 * @param userId 用户 ID
 * @param resumeText 简历文本
 * @param attemptCount 当前执行次数
 */
public record ResumeAnalysisExecutionBO(
        Long messageId,
        Long resumeId,
        Long userId,
        String resumeText,
        Integer attemptCount
) {
}
