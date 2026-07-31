package interview.resume.message;

import interview.common.constant.MessageEnvelope;
import interview.common.enums.MessageHandleStatus;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.resume.model.message.ResumeAnalysisCommand;
import interview.resume.service.ResumeAnalysisExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisHandlerTest {

    @Mock
    private ResumeAnalysisExecutionService executionService;

    @Test
    void handle_shouldRejectUnsupportedPayload() {
        ResumeAnalysisHandler handler =
                new ResumeAnalysisHandler(executionService, new ObjectMapper());
        MessageEnvelope message = new MessageEnvelope(
                20L, MsgTopic.RESUME_AI_PARSER, 2, "{}", "trace");

        MessageHandleResult result = handler.handle(message);

        assertEquals(MessageHandleStatus.PERMANENT_FAILURE, result.status());
    }

    @Test
    void handle_shouldDelegateValidCommand() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ResumeAnalysisCommand command =
                new ResumeAnalysisCommand(10L, 1L);
        String payload = objectMapper.writeValueAsString(command);
        MessageHandleResult success = MessageHandleResult.success();
        when(executionService.execute(20L, command)).thenReturn(success);
        ResumeAnalysisHandler handler =
                new ResumeAnalysisHandler(executionService, objectMapper);

        MessageHandleResult result = handler.handle(
                new MessageEnvelope(20L, MsgTopic.RESUME_AI_PARSER,
                        1, payload, "trace"));

        assertEquals(MessageHandleStatus.SUCCESS, result.status());
        verify(executionService).execute(20L, command);
    }
}
