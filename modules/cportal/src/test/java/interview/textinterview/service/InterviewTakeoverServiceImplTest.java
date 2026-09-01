package interview.textinterview.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewSessionStatus;
import interview.common.enums.InterviewType;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.mapper.InterviewTakeoverMapper;
import interview.textinterview.mapper.InterviewTimelineEventMapper;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.entity.InterviewTakeover;
import interview.textinterview.model.entity.InterviewTimelineEvent;
import interview.textinterview.model.enums.InterviewTakeoverEndAction;
import interview.textinterview.model.enums.InterviewTakeoverStatus;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.req.InterviewTakeoverEndReq;
import interview.textinterview.model.req.InterviewTakeoverStartReq;
import interview.textinterview.model.vo.InterviewSessionEndVO;
import interview.textinterview.model.vo.InterviewTakeoverEndVO;
import interview.textinterview.model.vo.InterviewTakeoverStartVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewTakeoverServiceImplTest {

    private static final long SESSION_ID = 9001L;
    private static final long SCHEDULE_ID = 37001L;
    private static final long ENTERPRISE_ID = 10L;
    private static final long INTERVIEWER_USER_ID = 5001L;
    private static final long TAKEOVER_ID = 35001L;

    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewScheduleQueryApi interviewScheduleQueryApi;
    @Mock
    private InterviewTimelineEventMapper timelineEventMapper;
    @Mock
    private InterviewSessionService interviewSessionService;
    @Mock
    private InterviewTakeoverMapper takeoverMapper;

    private InterviewTakeoverServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewSession"),
                InterviewSession.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewTakeover"),
                InterviewTakeover.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewTimelineEvent"),
                InterviewTimelineEvent.class);
        service = new InterviewTakeoverServiceImpl(
                sessionMapper, interviewScheduleQueryApi, timelineEventMapper, interviewSessionService);
        ReflectionTestUtils.setField(service, "baseMapper", takeoverMapper);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(INTERVIEWER_USER_ID)
                .userType(UserType.ENTERPRISE_USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    private InterviewSession session(InterviewSessionStatus status) {
        return InterviewSession.builder()
                .id(SESSION_ID)
                .enterpriseId(ENTERPRISE_ID)
                .userId(7001L)
                .scheduleId(SCHEDULE_ID)
                .sessionType(InterviewType.TEXT)
                .status(status)
                .build();
    }

    private InterviewScheduleQueryDTO schedule() {
        return new InterviewScheduleQueryDTO(
                SCHEDULE_ID, ENTERPRISE_ID, 200L, 7001L, 300L, 400L,
                (short) 1, "TECHNICAL", "技术面", INTERVIEWER_USER_ID,
                "候选人", "企业", "岗位",
                OffsetDateTime.now(), 60, InterviewType.TEXT,
                InterviewScheduleStatus.IN_PROGRESS, null, 0,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private InterviewTakeover takeover(InterviewTakeoverStatus status) {
        return InterviewTakeover.builder()
                .id(TAKEOVER_ID)
                .sessionId(SESSION_ID)
                .enterpriseId(ENTERPRISE_ID)
                .interviewerUserId(INTERVIEWER_USER_ID)
                .requestId("takeover-key-1")
                .status(status)
                .reason("需要确认项目经历")
                .startedAt(OffsetDateTime.now().minusMinutes(1))
                .endedAt(status == InterviewTakeoverStatus.ENDED ? OffsetDateTime.now() : null)
                .build();
    }

    private InterviewTakeoverStartReq startReq() {
        return new InterviewTakeoverStartReq("需要确认项目经历");
    }

    // ============================== startTakeover ==============================

    @Test
    void start_shouldCreateActiveTakeoverAndAppendTimelineEvent() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(takeoverMapper.selectOne(any())).thenReturn(null);
        when(takeoverMapper.selectCount(any())).thenReturn(0L);
        when(takeoverMapper.insert(any(InterviewTakeover.class))).thenReturn(1);
        when(timelineEventMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 1, false));

        InterviewTakeoverStartVO result = service.startTakeover(
                ENTERPRISE_ID, SESSION_ID, "takeover-key-1", startReq());

        assertEquals(InterviewTakeoverStatus.ACTIVE, result.status());
        assertEquals(SESSION_ID, result.sessionId());
        assertNotNull(result.startedAt());

        ArgumentCaptor<InterviewTakeover> captor = ArgumentCaptor.forClass(InterviewTakeover.class);
        verify(takeoverMapper).insert(captor.capture());
        assertEquals("takeover-key-1", captor.getValue().getRequestId());
        assertEquals(INTERVIEWER_USER_ID, captor.getValue().getInterviewerUserId());

        ArgumentCaptor<InterviewTimelineEvent> eventCaptor =
                ArgumentCaptor.forClass(InterviewTimelineEvent.class);
        verify(timelineEventMapper).insert(eventCaptor.capture());
        assertEquals("TAKEOVER_STARTED", eventCaptor.getValue().getEventType());
        assertEquals(1L, eventCaptor.getValue().getSequenceNum());
        assertEquals("INTERVIEWER", eventCaptor.getValue().getActorType());
    }

    @Test
    void start_shouldRejectWhenSessionNotInProgress() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.CREATED));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTakeover(ENTERPRISE_ID, SESSION_ID, "takeover-key-1", startReq()));

        assertEquals(ErrorCode.INTERVIEW_SESSION_STATUS_INVALID.getCode(), exception.getCode());
        verify(takeoverMapper, never()).insert(any(InterviewTakeover.class));
    }

    @Test
    void start_shouldRejectNonScheduledInterviewer() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        InterviewScheduleQueryDTO other = new InterviewScheduleQueryDTO(
                SCHEDULE_ID, ENTERPRISE_ID, 200L, 7001L, 300L, 400L,
                (short) 1, "TECHNICAL", "技术面", 9999L,
                "候选人", "企业", "岗位",
                OffsetDateTime.now(), 60, InterviewType.TEXT,
                InterviewScheduleStatus.IN_PROGRESS, null, 0,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(other);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTakeover(ENTERPRISE_ID, SESSION_ID, "takeover-key-1", startReq()));

        assertEquals(ErrorCode.PERMISSION_DENIED.getCode(), exception.getCode());
        verify(takeoverMapper, never()).insert(any(InterviewTakeover.class));
    }

    @Test
    void start_shouldRejectWhenActiveTakeoverExists() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(takeoverMapper.selectOne(any())).thenReturn(null);
        when(takeoverMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTakeover(ENTERPRISE_ID, SESSION_ID, "takeover-key-1", startReq()));

        assertEquals(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED.getCode(), exception.getCode());
        verify(takeoverMapper, never()).insert(any(InterviewTakeover.class));
    }

    @Test
    void start_shouldReplayWhenSameKeyAndSameReason() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(takeoverMapper.selectOne(any())).thenReturn(takeover(InterviewTakeoverStatus.ACTIVE));

        InterviewTakeoverStartVO result = service.startTakeover(
                ENTERPRISE_ID, SESSION_ID, "takeover-key-1", startReq());

        assertEquals(TAKEOVER_ID, result.takeoverId());
        assertEquals(InterviewTakeoverStatus.ACTIVE, result.status());
        verify(takeoverMapper, never()).insert(any(InterviewTakeover.class));
    }

    @Test
    void start_shouldRejectSameKeyWithDifferentReason() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(takeoverMapper.selectOne(any())).thenReturn(takeover(InterviewTakeoverStatus.ACTIVE));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTakeover(ENTERPRISE_ID, SESSION_ID, "takeover-key-1",
                        new InterviewTakeoverStartReq("另一个原因")));

        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.getCode(), exception.getCode());
    }

    // ============================== endTakeover ==============================

    @Test
    void end_shouldResumeAiWhenActionIsResumeAi() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(takeoverMapper.selectOne(any())).thenReturn(takeover(InterviewTakeoverStatus.ACTIVE));
        when(takeoverMapper.update(any(), any())).thenReturn(1);
        when(timelineEventMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 1, false));

        InterviewTakeoverEndVO result = service.endTakeover(
                ENTERPRISE_ID, SESSION_ID, TAKEOVER_ID,
                new InterviewTakeoverEndReq(InterviewTakeoverEndAction.RESUME_AI));

        assertEquals(InterviewTakeoverStatus.ENDED, result.status());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, result.sessionStatus());
        assertNotNull(result.endedAt());

        ArgumentCaptor<InterviewTimelineEvent> eventCaptor =
                ArgumentCaptor.forClass(InterviewTimelineEvent.class);
        verify(timelineEventMapper).insert(eventCaptor.capture());
        assertEquals("AI_RESUMED", eventCaptor.getValue().getEventType());
        verify(interviewSessionService, never()).endSession(anyLong(), any());
    }

    @Test
    void end_shouldEndSessionWhenActionIsEndSession() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(takeoverMapper.selectOne(any())).thenReturn(takeover(InterviewTakeoverStatus.ACTIVE));
        when(takeoverMapper.update(any(), any())).thenReturn(1);
        when(interviewSessionService.endSession(anyLong(), any())).thenReturn(
                new InterviewSessionEndVO(
                        SESSION_ID, InterviewType.TEXT, InterviewSessionStatus.COMPLETED,
                        60, null, null, OffsetDateTime.now()));
        when(timelineEventMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 1, false));

        InterviewTakeoverEndVO result = service.endTakeover(
                ENTERPRISE_ID, SESSION_ID, TAKEOVER_ID,
                new InterviewTakeoverEndReq(InterviewTakeoverEndAction.END_SESSION));

        assertEquals(InterviewTakeoverStatus.ENDED, result.status());
        assertEquals(InterviewSessionStatus.COMPLETED, result.sessionStatus());
        ArgumentCaptor<InterviewSessionEndReq> reqCaptor = ArgumentCaptor.forClass(InterviewSessionEndReq.class);
        verify(interviewSessionService).endSession(anyLong(), reqCaptor.capture());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, reqCaptor.getValue().expectedStatus());

        ArgumentCaptor<InterviewTimelineEvent> eventCaptor =
                ArgumentCaptor.forClass(InterviewTimelineEvent.class);
        verify(timelineEventMapper).insert(eventCaptor.capture());
        assertEquals("TAKEOVER_ENDED", eventCaptor.getValue().getEventType());
    }

    @Test
    void end_shouldReplayWhenAlreadyEnded() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.COMPLETED));
        when(takeoverMapper.selectOne(any())).thenReturn(takeover(InterviewTakeoverStatus.ENDED));

        InterviewTakeoverEndVO result = service.endTakeover(
                ENTERPRISE_ID, SESSION_ID, TAKEOVER_ID,
                new InterviewTakeoverEndReq(InterviewTakeoverEndAction.RESUME_AI));

        assertEquals(InterviewTakeoverStatus.ENDED, result.status());
        assertEquals(InterviewSessionStatus.COMPLETED, result.sessionStatus());
        verify(takeoverMapper, never()).update(any(), any());
        verify(timelineEventMapper, never()).insert(any(InterviewTimelineEvent.class));
    }

    @Test
    void end_shouldRejectWhenTakeoverNotFound() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));
        when(takeoverMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.endTakeover(ENTERPRISE_ID, SESSION_ID, TAKEOVER_ID,
                        new InterviewTakeoverEndReq(InterviewTakeoverEndAction.RESUME_AI)));

        assertEquals(ErrorCode.INTERVIEW_TAKEOVER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void end_shouldRejectWhenSessionNotInEnterprise() {
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session(InterviewSessionStatus.IN_PROGRESS));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.endTakeover(99L, SESSION_ID, TAKEOVER_ID,
                        new InterviewTakeoverEndReq(InterviewTakeoverEndAction.RESUME_AI)));

        assertEquals(ErrorCode.INTERVIEW_SESSION_NOT_FOUND.getCode(), exception.getCode());
    }
}
