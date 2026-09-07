package interview.textinterview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import interview.textinterview.mapper.InterviewAnswerMapper;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.mapper.InterviewTimelineEventMapper;
import interview.textinterview.model.command.InterviewQuestionRecordCommand;
import interview.textinterview.model.entity.InterviewAnswer;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.entity.InterviewTimelineEvent;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionTxServiceTest {

    @Mock
    private InterviewAnswerMapper interviewAnswerMapper;
    @Mock
    private InterviewSessionMapper interviewSessionMapper;
    @Mock
    private InterviewTimelineEventMapper interviewTimelineEventMapper;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewSession"),
                InterviewSession.class);
    }

    @Test
    void shouldPersistQuestionAndTimelineInOneUseCase() {
        InterviewQuestionTxService service = new InterviewQuestionTxService(
                interviewAnswerMapper, interviewSessionMapper, interviewTimelineEventMapper);
        InterviewQuestionRecordCommand command = new InterviewQuestionRecordCommand(
                9001L,
                10L,
                0,
                null,
                0,
                "请介绍一次性能优化经历",
                null,
                null,
                null
        );
        doAnswer(invocation -> {
            InterviewAnswer answer = invocation.getArgument(0);
            answer.setId(10001L);
            return 1;
        }).when(interviewAnswerMapper).insert(any(InterviewAnswer.class));
        when(interviewSessionMapper.update(isNull(), any())).thenReturn(1);

        Long answerId = service.recordQuestion(command);

        assertEquals(10001L, answerId);
        ArgumentCaptor<InterviewAnswer> answerCaptor = ArgumentCaptor.forClass(InterviewAnswer.class);
        verify(interviewAnswerMapper).insert(answerCaptor.capture());
        assertEquals(command.sessionId(), answerCaptor.getValue().getSessionId());
        assertEquals(command.content(), answerCaptor.getValue().getQuestionText());

        verify(interviewSessionMapper).update(isNull(), any());
        ArgumentCaptor<InterviewTimelineEvent> timelineCaptor =
                ArgumentCaptor.forClass(InterviewTimelineEvent.class);
        verify(interviewTimelineEventMapper).insert(timelineCaptor.capture());
        InterviewTimelineEvent timeline = timelineCaptor.getValue();
        assertEquals(command.sessionId(), timeline.getSessionId());
        assertEquals(1L, timeline.getSequenceNum());
        assertEquals("question.completed", timeline.getEventType());
        assertNotNull(timeline.getEventId());
    }
}
