package interview.resume.service;

import interview.api.aicore.ResumeAiApi;
import interview.api.aicore.dto.AiResumeAnalysisResult;
import interview.api.aicore.dto.LlmConfigSnapshotDTO;
import interview.common.enums.MessageHandleStatus;
import interview.common.spi.MessageHandleResult;
import interview.resume.model.bo.ResumeAnalysisExecutionBO;
import interview.resume.model.message.ResumeAnalysisCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisExecutionServiceTest {

    @Mock
    private ResumeAnalysisStateService stateService;
    @Mock
    private ResumeAiApi resumeAiApi;

    @Test
    void execute_shouldPersistResultAndComplete() {
        ResumeAnalysisExecutionBO execution =
                new ResumeAnalysisExecutionBO(
                        20L, 10L, 1L, "resume text", 1);
        when(stateService.claim(any(), any())).thenReturn(execution);
        when(resumeAiApi.analysisResume("resume text"))
                .thenReturn(validResult());
        when(stateService.complete(
                any(), any(), any(), any(), any())).thenReturn(true);

        MessageHandleResult result = service().execute(
                20L, new ResumeAnalysisCommand(10L, 1L));

        assertEquals(MessageHandleStatus.SUCCESS, result.status());
        verify(stateService).complete(
                any(), any(), any(), any(), any());
    }

    @Test
    void execute_shouldReturnRetryAfterFirstAiFailure() {
        ResumeAnalysisExecutionBO execution =
                new ResumeAnalysisExecutionBO(
                        20L, 10L, 1L, "resume text", 1);
        MessageHandleResult retry = MessageHandleResult.retry(
                OffsetDateTime.now().plusMinutes(1), "timeout");
        when(stateService.claim(any(), any())).thenReturn(execution);
        when(resumeAiApi.analysisResume("resume text"))
                .thenThrow(new RuntimeException("timeout"));
        when(stateService.fail(any(), any())).thenReturn(retry);

        MessageHandleResult result = service().execute(
                20L, new ResumeAnalysisCommand(10L, 1L));

        assertEquals(MessageHandleStatus.RETRY, result.status());
        verify(stateService).fail(any(), any());
    }

    @Test
    void execute_shouldResolveTaskThatWasNotClaimed() {
        ResumeAnalysisCommand command =
                new ResumeAnalysisCommand(10L, 1L);
        MessageHandleResult ignored =
                MessageHandleResult.ignored("completed");
        when(stateService.claim(20L, command)).thenReturn(null);
        when(stateService.resolveUnclaimed(20L, command))
                .thenReturn(ignored);

        MessageHandleResult result = service().execute(20L, command);

        assertEquals(MessageHandleStatus.IGNORED, result.status());
    }

    private AiResumeAnalysisResult validResult() {
        return new AiResumeAnalysisResult(
                80,
                List.of("工程经验完整"),
                List.of("补充量化成果"),
                LlmConfigSnapshotDTO.builder().schemaVersion(1).build());
    }

    private ResumeAnalysisExecutionService service() {
        return new ResumeAnalysisExecutionService(
                stateService, resumeAiApi, new ObjectMapper());
    }
}
