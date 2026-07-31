package interview.resume.message;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.resume.model.message.CandidateProfileCommand;
import interview.resume.service.CandidateProfileExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 人才画像生成兜底消息处理器。
 */
@Component
@RequiredArgsConstructor
public class CandidateProfileHandler implements MessageHandler {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final CandidateProfileExecutionService executionService;
    private final ObjectMapper objectMapper;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.CANDIDATE_PROFILE_GENERATION;
    }

    @Override
    public MessageHandleResult handle(MessageEnvelope message) {
        CandidateProfileCommand command = parseCommand(message);
        if (command == null) {
            return MessageHandleResult.permanentFailure(
                    "invalid candidate profile payload");
        }
        return executionService.execute(message.messageId(), command);
    }

    private CandidateProfileCommand parseCommand(MessageEnvelope message) {
        if (message.schemaVersion() == null
                || message.schemaVersion() != SUPPORTED_SCHEMA_VERSION
                || StrUtil.isBlank(message.payload())) {
            return null;
        }
        try {
            CandidateProfileCommand command = objectMapper.readValue(
                    message.payload(), CandidateProfileCommand.class);
            if (command == null || command.resumeId() == null
                    || command.candidateId() == null) {
                return null;
            }
            return command;
        } catch (Exception ignored) {
            return null;
        }
    }
}
