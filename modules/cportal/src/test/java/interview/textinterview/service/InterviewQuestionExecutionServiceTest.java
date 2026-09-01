package interview.textinterview.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import interview.api.aicore.InterviewQuestionAiApi;
import interview.api.aicore.dto.InterviewQuestionGeneratedResultDTO;
import interview.api.aicore.dto.InterviewQuestionGenerationReqDTO;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;
import interview.common.enums.QuestionKind;
import interview.textinterview.event.InterviewSessionReadyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionExecutionServiceTest {

    private static final long SESSION_ID = 9001L;
    private static final long SCHEDULE_ID = 37001L;
    private static final long ENTERPRISE_ID = 10L;

    @Mock
    private InterviewScheduleQueryApi interviewScheduleQueryApi;
    @Mock
    private InterviewQuestionAiApi interviewQuestionAiApi;
    @Mock
    private InterviewSessionService interviewSessionService;

    @Test
    void shouldBuildQuestionContextAndPersistGeneratedQuestion() {
        InterviewQuestionExecutionService service = new InterviewQuestionExecutionService(
                interviewScheduleQueryApi, interviewQuestionAiApi, interviewSessionService);
        InterviewQuestionGeneratedResultDTO generated = new InterviewQuestionGeneratedResultDTO(
                "请介绍一次性能优化经历", "性能分析", "HARD", "FIRST", "model-response");
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(interviewScheduleQueryApi.getTemplateSnapshot(SCHEDULE_ID)).thenReturn("""
                {"stages":[{"phaseCode":"TECHNICAL","difficulty":"HARD",
                "promptOverride":"聚焦工程实践","focusPoints":["Java","SQL"]}]}
                """);
        when(interviewQuestionAiApi.generateQuestion(any())).thenReturn(generated);
        when(interviewSessionService.recordAiQuestion(
                SESSION_ID, ENTERPRISE_ID, 0, null, 0, generated)).thenReturn(10001L);

        service.generateFirstQuestion(event());

        ArgumentCaptor<InterviewQuestionGenerationReqDTO> requestCaptor =
                ArgumentCaptor.forClass(InterviewQuestionGenerationReqDTO.class);
        verify(interviewQuestionAiApi).generateQuestion(requestCaptor.capture());
        InterviewQuestionGenerationReqDTO request = requestCaptor.getValue();
        assertEquals("TECHNICAL", request.phaseCode());
        assertEquals("HARD", request.difficulty());
        assertEquals("聚焦工程实践", request.promptOverride());
        assertEquals(List.of("Java", "SQL"), request.focusPoints());
        assertEquals("Java 后端工程师", request.jobTitle());
        verify(interviewSessionService).recordAiQuestion(
                SESSION_ID, ENTERPRISE_ID, 0, null, 0, generated);
    }

    @Test
    void shouldNotPersistWhenAiReturnsBlankQuestion() {
        InterviewQuestionExecutionService service = new InterviewQuestionExecutionService(
                interviewScheduleQueryApi, interviewQuestionAiApi, interviewSessionService);
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(interviewScheduleQueryApi.getTemplateSnapshot(SCHEDULE_ID)).thenReturn(null);
        when(interviewQuestionAiApi.generateQuestion(any())).thenReturn(
                new InterviewQuestionGeneratedResultDTO(" ", null, null, null, null));

        service.generateFirstQuestion(event());

        verify(interviewSessionService, never()).recordAiQuestion(
                anyLong(), anyLong(), anyInt(), isNull(), anyInt(), any());
    }

    private InterviewSessionReadyEvent event() {
        return new InterviewSessionReadyEvent(
                SESSION_ID, SCHEDULE_ID, ENTERPRISE_ID,
                InterviewType.TEXT, QuestionKind.FIRST, "trace-1");
    }

    private InterviewScheduleQueryDTO schedule() {
        OffsetDateTime now = OffsetDateTime.now();
        return new InterviewScheduleQueryDTO(
                SCHEDULE_ID,
                ENTERPRISE_ID,
                2001L,
                3001L,
                4001L,
                5001L,
                (short) 1,
                "TECHNICAL",
                "技术面",
                6001L,
                "候选人",
                "示例企业",
                "Java 后端工程师",
                now.plusHours(1),
                60,
                InterviewType.TEXT,
                InterviewScheduleStatus.CONFIRMED,
                null,
                0,
                now,
                now
        );
    }
}
