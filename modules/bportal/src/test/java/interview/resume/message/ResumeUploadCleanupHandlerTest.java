package interview.resume.message;

import interview.api.infra.FileStorageApi;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MessageHandleStatus;
import interview.common.enums.MsgTopic;
import interview.common.exception.BusinessException;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.message.ResumeUploadCleanupCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

import static interview.common.enums.ErrorCode.FILE_DELETE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ResumeUploadCleanupHandlerTest {

    private ResumesMapper resumesMapper;
    private FileStorageApi fileStorageApi;
    private ResumeUploadCleanupHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resumesMapper = mock(ResumesMapper.class);
        fileStorageApi = mock(FileStorageApi.class);
        objectMapper = new ObjectMapper();
        handler = new ResumeUploadCleanupHandler(resumesMapper, fileStorageApi, objectMapper);
    }

    @Test
    void handle_shouldDeleteOnlyFailedAndDeletedResumeObject() {
        // 只有上传失败且已释放配额的简历允许删除对象。
        Resumes resume = resume(AnalyzeStatus.UPLOAD_FAILED, true);
        when(resumesMapper.selectIncludingDeletedById(1L)).thenReturn(resume);
        when(resumesMapper.existsOtherAliveByStorageUrl("resume/1.pdf", 1L)).thenReturn(false);

        var result = handler.handle(message());

        assertEquals(MessageHandleStatus.SUCCESS, result.status());
        verify(fileStorageApi).deleteFile("resume/1.pdf");
    }

    @Test
    void handle_shouldRetryWhileResumeIsUploading() {
        // 清理消息不得抢在超时修复事务之前删除对象。
        Resumes resume = resume(AnalyzeStatus.UPLOADING, false);
        resume.setUploadDeadlineAt(OffsetDateTime.now().plusMinutes(10));
        when(resumesMapper.selectIncludingDeletedById(1L)).thenReturn(resume);

        var result = handler.handle(message());

        assertEquals(MessageHandleStatus.RETRY, result.status());
        verifyNoInteractions(fileStorageApi);
    }

    @Test
    void handle_shouldIgnoreActiveResume() {
        // 延迟到达的补偿消息不能删除已经确认成功的简历。
        when(resumesMapper.selectIncludingDeletedById(1L))
                .thenReturn(resume(AnalyzeStatus.PENDING, false));

        var result = handler.handle(message());

        assertEquals(MessageHandleStatus.IGNORED, result.status());
        verifyNoInteractions(fileStorageApi);
    }

    @Test
    void handle_shouldRejectBindingMismatch() {
        // 消息必须与数据库中的用户和对象键同时匹配。
        Resumes resume = resume(AnalyzeStatus.UPLOAD_FAILED, true);
        resume.setUserId(99L);
        when(resumesMapper.selectIncludingDeletedById(1L)).thenReturn(resume);

        var result = handler.handle(message());

        assertEquals(MessageHandleStatus.PERMANENT_FAILURE, result.status());
        verifyNoInteractions(fileStorageApi);
    }

    @Test
    void handle_shouldPropagateStorageFailureForFrameworkRetry() {
        // 存储删除异常必须交给租约调度框架重试。
        when(resumesMapper.selectIncludingDeletedById(1L))
                .thenReturn(resume(AnalyzeStatus.UPLOAD_FAILED, true));
        when(resumesMapper.existsOtherAliveByStorageUrl("resume/1.pdf", 1L)).thenReturn(false);
        doThrow(new BusinessException(FILE_DELETE_FAILED))
                .when(fileStorageApi).deleteFile("resume/1.pdf");

        assertThrows(BusinessException.class, () -> handler.handle(message()));
    }

    private MessageEnvelope message() {
        ResumeUploadCleanupCommand command = new ResumeUploadCleanupCommand(1L, 10L, "resume/1.pdf");
        return MessageEnvelope.builder()
                .messageId(100L)
                .topic(MsgTopic.RESUME_UPLOAD_CLEANUP)
                .schemaVersion(1)
                .payload(objectMapper.writeValueAsString(command))
                .build();
    }

    private Resumes resume(AnalyzeStatus status, boolean deleted) {
        return Resumes.builder()
                .id(1L)
                .userId(10L)
                .storageUrl("resume/1.pdf")
                .analyzeStatus(status)
                .isDeleted(deleted)
                .build();
    }
}
