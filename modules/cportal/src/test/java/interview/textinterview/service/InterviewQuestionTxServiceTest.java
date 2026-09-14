package interview.textinterview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONUtil;
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
        when(interviewSessionMapper.selectOne(any())).thenReturn(sessionWithSequence(1L));

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

    @Test
    void shouldAllocateNextSequenceAndCarryParentAnswerForFollowUp() {
        InterviewQuestionTxService service = new InterviewQuestionTxService(
                interviewAnswerMapper, interviewSessionMapper, interviewTimelineEventMapper);
        // 追问题：父作答 10001、题目序号 1、追问深度 1。
        InterviewQuestionRecordCommand command = new InterviewQuestionRecordCommand(
                9001L,
                10L,
                1,
                10001L,
                1,
                "请进一步说明量化收益",
                "FOLLOW_UP",
                "性能量化",
                "HARD"
        );
        doAnswer(invocation -> {
            InterviewAnswer answer = invocation.getArgument(0);
            answer.setId(10002L);
            return 1;
        }).when(interviewAnswerMapper).insert(any(InterviewAnswer.class));
        when(interviewSessionMapper.update(isNull(), any())).thenReturn(1);
        // 会话已有 1 条事件，本次分配应得到序号 2 而非复用 1。
        when(interviewSessionMapper.selectOne(any())).thenReturn(sessionWithSequence(2L));

        assertEquals(10002L, service.recordQuestion(command));

        ArgumentCaptor<InterviewAnswer> answerCaptor = ArgumentCaptor.forClass(InterviewAnswer.class);
        verify(interviewAnswerMapper).insert(answerCaptor.capture());
        assertEquals(10001L, answerCaptor.getValue().getParentMessageId());
        assertEquals(1, answerCaptor.getValue().getFollowUpDepth());

        ArgumentCaptor<InterviewTimelineEvent> timelineCaptor =
                ArgumentCaptor.forClass(InterviewTimelineEvent.class);
        verify(interviewTimelineEventMapper).insert(timelineCaptor.capture());
        InterviewTimelineEvent timeline = timelineCaptor.getValue();
        assertEquals(2L, timeline.getSequenceNum());
        assertEquals("FOLLOW_UP", JSONUtil.parseObj(timeline.getPayloadJson()).getStr("questionKind"));
        assertEquals(10001L, JSONUtil.parseObj(timeline.getPayloadJson()).getLong("parentAnswerId"));
    }

    private InterviewSession sessionWithSequence(Long sequence) {
        InterviewSession session = new InterviewSession();
        session.setId(9001L);
        session.setLastEventSequence(sequence);
        return session;
    }
}
