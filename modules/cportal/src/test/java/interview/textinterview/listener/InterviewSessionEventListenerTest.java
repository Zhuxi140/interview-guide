package interview.textinterview.listener;

import org.junit.jupiter.api.Test;

import interview.common.enums.InterviewType;
import interview.common.enums.QuestionKind;
import interview.textinterview.event.InterviewAnswerSubmittedEvent;
import interview.textinterview.event.InterviewSessionReadyEvent;
import interview.textinterview.service.InterviewQuestionExecutionService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InterviewSessionEventListenerTest {

    @Test
    void shouldDelegateSessionReadyEventToExecutionService() {
        InterviewQuestionExecutionService executionService = mock(InterviewQuestionExecutionService.class);
        InterviewSessionEventListener listener = new InterviewSessionEventListener(executionService);
        InterviewSessionReadyEvent event = new InterviewSessionReadyEvent(
                9001L, 37001L, 10L, InterviewType.TEXT, QuestionKind.FIRST, "trace-1");

        listener.handleSessionReady(event);

        verify(executionService).generateFirstQuestion(event);
    }

    @Test
    void shouldDelegateAnswerSubmittedEventToExecutionService() {
        InterviewQuestionExecutionService executionService = mock(InterviewQuestionExecutionService.class);
        InterviewSessionEventListener listener = new InterviewSessionEventListener(executionService);
        InterviewAnswerSubmittedEvent event = new InterviewAnswerSubmittedEvent(
                9001L, 37001L, 10L, 20001L, 0, 0,
                "请介绍一次性能优化经历", "我做过一次缓存优化", "trace-1");

        listener.handleAnswerSubmitted(event);

        verify(executionService).generateFollowUpQuestion(event);
    }
}
