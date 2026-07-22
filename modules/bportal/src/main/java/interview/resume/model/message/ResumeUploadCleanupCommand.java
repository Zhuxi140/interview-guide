package interview.resume.model.message;

/**
 * 简历上传失败后的对象清理命令。
 *
 * @param resumeId 简历 ID
 * @param userId 用户 ID
 * @param objectKey 对象存储键
 */
public record ResumeUploadCleanupCommand(
        Long resumeId,
        Long userId,
        String objectKey
) {
}
