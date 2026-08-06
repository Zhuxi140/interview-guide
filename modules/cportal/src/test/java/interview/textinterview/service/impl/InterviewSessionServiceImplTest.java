package interview.textinterview.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.api.bportal.InterviewScheduleCommandApi;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewSessionStatus;
import interview.common.enums.InterviewType;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.InterviewReportMapper;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.model.entity.InterviewReport;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.InterviewJoinTokenVO;
import interview.textinterview.model.vo.InterviewSessionEndVO;
import interview.voiceinterview.mapper.VoiceInterviewSessionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewSessionServiceImplTest {

    private static final long SCHEDULE_ID = 37001L;
    private static final long SESSION_ID = 9001L;
    private static final long ENTERPRISE_ID = 10L;
    private static final long CANDIDATE_USER_ID = 5001L;
    private static final long INTERVIEWER_USER_ID = 7001L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private InterviewScheduleQueryApi interviewScheduleQueryApi;
    @Mock
    private InterviewScheduleCommandApi interviewScheduleCommandApi;
    @Mock
    private VoiceInterviewSessionMapper voiceInterviewSessionMapper;
    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewReportMapper reportMapper;

    private InterviewSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        // 注册实体元数据，使 lambda select 能解析列名（Spring 启动时会自动完成，单测需手动）。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewSession"),
                InterviewSession.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewReport"),
                InterviewReport.class);
        service = new InterviewSessionServiceImpl(
                voiceInterviewSessionMapper, null, reportMapper, interviewScheduleQueryApi,
                interviewScheduleCommandApi, stringRedisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", sessionMapper);
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    private InterviewScheduleQueryDTO schedule() {
        return new InterviewScheduleQueryDTO(
                SCHEDULE_ID, ENTERPRISE_ID, 200L, CANDIDATE_USER_ID, 300L, 400L,
                (short) 1, "TECHNICAL", "技术面", INTERVIEWER_USER_ID,
                "候选人", "企业", "岗位",
                OffsetDateTime.now().minusMinutes(1), 60, InterviewType.TEXT,
                InterviewScheduleStatus.CONFIRMED, null, 1,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void generateJoinTokenCreatesSessionAndTokenForCandidate() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(CANDIDATE_USER_ID)
                .userType(UserType.CANDIDATE)
                .build());
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());
        when(sessionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            InterviewSession entity = invocation.getArgument(0);
            entity.setId(9001L);
            return 1;
        }).when(sessionMapper).insert(any(InterviewSession.class));
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        InterviewJoinTokenVO vo = service.generateJoinToken(SCHEDULE_ID, "key-1");

        assertEquals(InterviewType.TEXT, vo.sessionType());
        assertEquals(InterviewSessionStatus.CREATED, vo.status());
        assertEquals((short) 1, vo.attemptNo());
        assertEquals(0L, vo.lastEventSequence());
        assertEquals("/ws/v1/interview-sessions/" + vo.sessionId(), vo.wsUrl());
        assertEquals(60, vo.expiresInSeconds());
        assertFalse(vo.connectionToken().isBlank());
        verify(interviewScheduleCommandApi).startSchedule(SCHEDULE_ID, ENTERPRISE_ID);
    }

    @Test
    void generateJoinTokenRejectsUnauthorizedUser() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(999L)
                .userType(UserType.CANDIDATE)
                .build());
        when(interviewScheduleQueryApi.getSchedule(SCHEDULE_ID)).thenReturn(schedule());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generateJoinToken(SCHEDULE_ID, "key-1"));

        assertEquals(ErrorCode.USER_NOT_PARTICIPANT.getCode(), ex.getCode());
    }

    @Test
    void consumeConnectionTokenReturnsNullWhenMissing() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        assertNull(service.consumeConnectionToken("missing-token"));
    }

    @Test
    void endSessionCompletesSessionAndCreatesPendingReport() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(CANDIDATE_USER_ID)
                .userType(UserType.CANDIDATE)
                .build());
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS,
                OffsetDateTime.now().minusMinutes(10), null);
        when(sessionMapper.selectOne(any(Wrapper.class))).thenReturn(session);
        when(sessionMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(reportMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        InterviewSessionEndVO vo = service.endSession(SESSION_ID,
                new InterviewSessionEndReq(InterviewSessionStatus.IN_PROGRESS));

        assertEquals(SESSION_ID, vo.sessionId());
        assertEquals(InterviewSessionStatus.COMPLETED, vo.status());
        assertEquals(600, vo.actualDurationSeconds());
        assertEquals(InterviewReportGenerationStatus.PENDING, vo.reportStatus());
        assertNull(vo.voiceEvaluationStatus());
        verify(interviewScheduleCommandApi).completeSchedule(SCHEDULE_ID, ENTERPRISE_ID);
        verify(reportMapper).insert(any(InterviewReport.class));
    }

    @Test
    void endSessionIsIdempotentWhenAlreadyCompleted() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(CANDIDATE_USER_ID)
                .userType(UserType.CANDIDATE)
                .build());
        InterviewSession session = session(InterviewSessionStatus.COMPLETED,
                OffsetDateTime.now().minusMinutes(10), OffsetDateTime.now().minusMinutes(1));
        when(sessionMapper.selectOne(any(Wrapper.class))).thenReturn(session);
        when(reportMapper.selectOne(any(Wrapper.class))).thenReturn(report());

        InterviewSessionEndVO vo = service.endSession(SESSION_ID,
                new InterviewSessionEndReq(InterviewSessionStatus.IN_PROGRESS));

        assertEquals(InterviewSessionStatus.COMPLETED, vo.status());
        assertEquals(InterviewReportGenerationStatus.PENDING, vo.reportStatus());
        verify(interviewScheduleCommandApi, never()).completeSchedule(anyLong(), anyLong());
        verify(reportMapper, never()).insert(any(InterviewReport.class));
    }

    @Test
    void endSessionRejectsExpectedStatusMismatch() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(CANDIDATE_USER_ID)
                .userType(UserType.CANDIDATE)
                .build());
        when(sessionMapper.selectOne(any(Wrapper.class)))
                .thenReturn(session(InterviewSessionStatus.IN_PROGRESS, null, null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.endSession(SESSION_ID,
                        new InterviewSessionEndReq(InterviewSessionStatus.COMPLETED)));

        assertEquals(ErrorCode.INTERVIEW_SESSION_STATUS_INVALID.getCode(), ex.getCode());
        verify(interviewScheduleCommandApi, never()).completeSchedule(anyLong(), anyLong());
    }

    @Test
    void endSessionRejectsNonParticipant() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(999L)
                .userType(UserType.CANDIDATE)
                .build());
        when(sessionMapper.selectOne(any(Wrapper.class))).thenReturn(
                session(InterviewSessionStatus.IN_PROGRESS, null, null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.endSession(SESSION_ID,
                        new InterviewSessionEndReq(InterviewSessionStatus.IN_PROGRESS)));

        assertEquals(ErrorCode.INTERVIEW_SESSION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void endSessionReturnsExistingResultOnConcurrentUpdateLoss() {
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(CANDIDATE_USER_ID)
                .userType(UserType.CANDIDATE)
                .build());
        InterviewSession inProgress = session(InterviewSessionStatus.IN_PROGRESS,
                OffsetDateTime.now().minusMinutes(5), null);
        InterviewSession completed = session(InterviewSessionStatus.COMPLETED,
                OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now());
        when(sessionMapper.selectOne(any(Wrapper.class)))
                .thenReturn(inProgress, completed);
        when(sessionMapper.update(any(), any(Wrapper.class))).thenReturn(0);
        when(reportMapper.selectOne(any(Wrapper.class))).thenReturn(report());

        InterviewSessionEndVO vo = service.endSession(SESSION_ID,
                new InterviewSessionEndReq(InterviewSessionStatus.IN_PROGRESS));

        assertEquals(InterviewSessionStatus.COMPLETED, vo.status());
        assertEquals(300, vo.actualDurationSeconds());
    }

    private InterviewSession session(InterviewSessionStatus status,
                                     OffsetDateTime startedAt, OffsetDateTime endedAt) {
        return InterviewSession.builder()
                .id(SESSION_ID)
                .enterpriseId(ENTERPRISE_ID)
                .userId(CANDIDATE_USER_ID)
                .scheduleId(SCHEDULE_ID)
                .sessionType(InterviewType.TEXT)
                .attemptNo((short) 1)
                .status(status)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();
    }

    private InterviewReport report() {
        return InterviewReport.builder()
                .id(1L)
                .enterpriseId(ENTERPRISE_ID)
                .scheduleId(SCHEDULE_ID)
                .generationStatus(InterviewReportGenerationStatus.PENDING)
                .build();
    }
}