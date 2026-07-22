package interview.resume.message;

import interview.api.infra.dto.MessageDTO;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.resume.model.entity.Resumes;
import interview.resume.model.message.ResumeUploadCleanupCommand;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

/**
 * 统一生成简历上传清理消息，避免生产与修复链路协议漂移。
 */
@Component
public class ResumeCleanupMessageFactory {

    private static final int MESSAGE_SCHEMA_VERSION = 1;
    private static final int CLEANUP_MAX_RETRIES = 5;

    private final ObjectMapper objectMapper;

    public ResumeCleanupMessageFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 创建简历上传清理消息
     * @param resume 简历记录
     * @param executeAt 首次或下次执行时间
     * @return 清理消息
     */
    public MessageDTO create(Resumes resume, OffsetDateTime executeAt) {
        // 业务幂等键与消息载荷都绑定简历 ID。
        ResumeUploadCleanupCommand command = new ResumeUploadCleanupCommand(
                resume.getId(), resume.getUserId(), resume.getStorageUrl()
        );
        return MessageDTO.builder()
                .topic(MsgTopic.RESUME_UPLOAD_CLEANUP)
                .bizKey(MsgTopic.RESUME_UPLOAD_CLEANUP.name() + ":" + resume.getId())
                .schemaVersion(MESSAGE_SCHEMA_VERSION)
                .status(MsgStatus.PENDING)
                .payload(objectMapper.writeValueAsString(command))
                .lastError("上传简历，提前保存清理补偿意图")
                .priority(MsgPriority.LOW)
                .maxRetries(CLEANUP_MAX_RETRIES)
                .nextRetryAt(executeAt)
                .build();
    }
}
