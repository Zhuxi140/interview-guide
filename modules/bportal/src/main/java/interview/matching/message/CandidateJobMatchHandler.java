package interview.matching.message;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.matching.model.message.CandidateJobMatchCommand;
import interview.matching.service.CandidateJobMatchExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 候选人岗位适配预测兜底消息处理器。
 */
@Component
@RequiredArgsConstructor
public class CandidateJobMatchHandler implements MessageHandler {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final CandidateJobMatchExecutionService executionService;
    private final ObjectMapper objectMapper;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.CANDIDATE_JOB_MATCHING;
    }

    @Override
    public MessageHandleResult handle(MessageEnvelope message) {
        CandidateJobMatchCommand command = parseCommand(message);
        if (command == null) {
            return MessageHandleResult.permanentFailure(
                    "invalid candidate job match payload");
        }
        return executionService.execute(message.messageId(), command);
    }

    private CandidateJobMatchCommand parseCommand(MessageEnvelope message) {
        if (message.schemaVersion() == null
                || message.schemaVersion() != SUPPORTED_SCHEMA_VERSION
                || StrUtil.isBlank(message.payload())) {
            return null;
        }
        try {
            CandidateJobMatchCommand command = objectMapper.readValue(
                    message.payload(), CandidateJobMatchCommand.class);
            return command == null || command.applicationId() == null
                    ? null : command;
        } catch (Exception ignored) {
            return null;
        }
    }
}
