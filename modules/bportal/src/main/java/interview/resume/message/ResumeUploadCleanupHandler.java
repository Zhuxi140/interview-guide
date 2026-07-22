package interview.resume.message;

import cn.hutool.core.util.StrUtil;
import interview.api.infra.FileStorageApi;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.message.ResumeUploadCleanupCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 简历上传失败后的业务清理判定器。
 */
@Component
@RequiredArgsConstructor
public class ResumeUploadCleanupHandler implements MessageHandler {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final long CLEANUP_GRACE_MINUTES = 5;

    private final ResumesMapper resumesMapper;
    private final FileStorageApi fileStorageApi;
    private final ObjectMapper objectMapper;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.RESUME_UPLOAD_CLEANUP;
    }

    @Override
    public MessageHandleResult handle(MessageEnvelope message) {
        // 先验证协议版本和结构，异常消息不得退化为按 URL 盲删。
        ResumeUploadCleanupCommand command = parseCommand(message);
        if (command == null) {
            return MessageHandleResult.permanentFailure("invalid resume upload cleanup payload");
        }

        // 查询包含逻辑删除数据的业务记录并核对消息绑定关系。
        Resumes resume = resumesMapper.selectIncludingDeletedById(command.resumeId());
        if (resume == null
                || !Objects.equals(resume.getUserId(), command.userId())
                || !Objects.equals(resume.getStorageUrl(), command.objectKey())) {
            return MessageHandleResult.permanentFailure("resume cleanup binding mismatch");
        }

        // 上传尚未被修复任务判定失败时只延迟，不抢先删除对象。
        if (resume.getAnalyzeStatus() == AnalyzeStatus.UPLOADING) {
            OffsetDateTime retryAt = resume.getUploadDeadlineAt() == null
                    ? OffsetDateTime.now().plusMinutes(1)
                    : resume.getUploadDeadlineAt().plusMinutes(CLEANUP_GRACE_MINUTES);
            if (!retryAt.isAfter(OffsetDateTime.now())) {
                retryAt = OffsetDateTime.now().plusMinutes(1);
            }
            return MessageHandleResult.retry(retryAt, "resume is still uploading");
        }

        // 所有正常业务状态均视为清理意图失效。
        if (resume.getAnalyzeStatus() != AnalyzeStatus.UPLOAD_FAILED) {
            return MessageHandleResult.ignored("resume is active");
        }
        if (!Boolean.TRUE.equals(resume.getIsDeleted())) {
            return MessageHandleResult.permanentFailure("failed resume is not logically deleted");
        }
        if (resumesMapper.existsOtherAliveByStorageUrl(command.objectKey(), command.resumeId())) {
            return MessageHandleResult.ignored("object key is referenced by another active resume");
        }

        // 对象删除必须幂等；存储异常由上层租约调度器统一重试。
        fileStorageApi.deleteFile(command.objectKey());
        return MessageHandleResult.success();
    }

    private ResumeUploadCleanupCommand parseCommand(MessageEnvelope message) {
        if (message.schemaVersion() == null
                || message.schemaVersion() != SUPPORTED_SCHEMA_VERSION
                || StrUtil.isBlank(message.payload())) {
            return null;
        }
        try {
            ResumeUploadCleanupCommand command = objectMapper.readValue(
                    message.payload(), ResumeUploadCleanupCommand.class
            );
            if (command == null || command.resumeId() == null
                    || command.userId() == null || StrUtil.isBlank(command.objectKey())) {
                return null;
            }
            return command;
        } catch (Exception ignored) {
            return null;
        }
    }
}
