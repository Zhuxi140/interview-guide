package interview.matching.message;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.matching.model.message.ApplicationScreeningCommand;
import interview.matching.service.ApplicationScreeningExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * HR AI 初筛兜底消息处理器。
 */
@Component
@RequiredArgsConstructor
public class ApplicationScreeningHandler implements MessageHandler {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ApplicationScreeningExecutionService executionService;
    private final ObjectMapper objectMapper;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.HR_APPLICATION_SCREENING;
    }

    @Override
    public MessageHandleResult handle(MessageEnvelope message) {
        ApplicationScreeningCommand command = parseCommand(message);
        if (command == null) {
            return MessageHandleResult.permanentFailure(
                    "invalid application screening payload");
        }
        return executionService.execute(message.messageId(), command);
    }

    private ApplicationScreeningCommand parseCommand(MessageEnvelope message) {
        if (message.schemaVersion() == null
                || message.schemaVersion() != SUPPORTED_SCHEMA_VERSION
                || StrUtil.isBlank(message.payload())) {
            return null;
        }
        try {
            ApplicationScreeningCommand command = objectMapper.readValue(
                    message.payload(), ApplicationScreeningCommand.class);
            return command == null || command.applicationId() == null
                    ? null : command;
        } catch (Exception ignored) {
            return null;
        }
    }
}
