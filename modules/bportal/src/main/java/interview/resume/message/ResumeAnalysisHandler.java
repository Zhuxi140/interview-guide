package interview.resume.message;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.resume.model.message.ResumeAnalysisCommand;
import interview.resume.service.ResumeAnalysisExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 简历 AI 分析兜底消息处理器。
 */
@Component
@RequiredArgsConstructor
public class ResumeAnalysisHandler implements MessageHandler {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ResumeAnalysisExecutionService executionService;
    private final ObjectMapper objectMapper;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.RESUME_AI_PARSER;
    }

    @Override
    public MessageHandleResult handle(MessageEnvelope message) {
        ResumeAnalysisCommand command = parseCommand(message);
        if (command == null) {
            return MessageHandleResult.permanentFailure(
                    "invalid resume analysis payload");
        }
        return executionService.execute(message.messageId(), command);
    }

    private ResumeAnalysisCommand parseCommand(MessageEnvelope message) {
        if (message.schemaVersion() == null
                || message.schemaVersion() != SUPPORTED_SCHEMA_VERSION
                || StrUtil.isBlank(message.payload())) {
            return null;
        }
        try {
            ResumeAnalysisCommand command = objectMapper.readValue(
                    message.payload(), ResumeAnalysisCommand.class);
            if (command == null || command.resumeId() == null
                    || command.userId() == null) {
                return null;
            }
            return command;
        } catch (Exception ignored) {
            return null;
        }
    }
}
