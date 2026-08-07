package interview.interviewcfg.message;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.interviewcfg.service.InterviewPlanGenerationExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Agent 面试编排草案生成兜底消息处理器。
 */
@Component
@RequiredArgsConstructor
public class InterviewPlanGenerationHandler implements MessageHandler {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final InterviewPlanGenerationExecutionService executionService;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.INTERVIEW_PLAN_GENERATION;
    }

    @Override
    public MessageHandleResult handle(MessageEnvelope message) {
        Long draftId = parseDraftId(message);
        if (draftId == null) {
            return MessageHandleResult.permanentFailure(
                    "invalid interview plan generation payload");
        }
        return executionService.execute(message.messageId(), draftId);
    }

    private Long parseDraftId(MessageEnvelope message) {
        if (message.schemaVersion() == null
                || message.schemaVersion() != SUPPORTED_SCHEMA_VERSION
                || StrUtil.isBlank(message.payload())) {
            return null;
        }
        try {
            return Long.parseLong(message.payload().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}