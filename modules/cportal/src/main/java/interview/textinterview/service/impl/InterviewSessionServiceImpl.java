package interview.textinterview.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.bportal.InterviewScheduleCommandApi;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.JobValidationApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.constant.InterviewConnectionContext;
import interview.common.constant.InterviewConnectionKeyConstant;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewSessionStatus;
import interview.common.enums.InterviewType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.framework.redis.RedisScripts;
import interview.textinterview.mapper.InterviewReportMapper;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.mapper.InterviewTimelineEventMapper;
import interview.textinterview.model.entity.InterviewReport;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.entity.InterviewTimelineEvent;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.*;
import interview.textinterview.service.InterviewSessionService;
import interview.voiceinterview.mapper.VoiceInterviewSessionMapper;
import interview.voiceinterview.model.entity.VoiceInterviewSession;
import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
import interview.voiceinterview.model.vo.VoiceDetailsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cn.hutool.json.JSONUtil.toBean;
import static cn.hutool.json.JSONUtil.toJsonStr;

@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession>
        implements InterviewSessionService {

    private static final int CONNECTION_TOKEN_TTL_SECONDS = 60;
    private final VoiceInterviewSessionMapper voiceInterviewSessionMapper;
    private final InterviewTimelineEventMapper interviewTimelineEventMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final InterviewScheduleQueryApi interviewScheduleQueryApi;
    private final InterviewScheduleCommandApi interviewScheduleCommandApi;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public InterviewJoinTokenVO generateJoinToken(Long scheduleId, String idempotencyKey) {
        // 从 AuthContext 获取用户，并通过 InterviewScheduleQueryApi 校验其为候选人或当前企业面试官。
        Long userId = AuthContext.getRequiredUserId();
        InterviewScheduleQueryDTO scheduleDTO = interviewScheduleQueryApi.getSchedule(scheduleId);
        if (scheduleDTO == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        Long candidateUserId = scheduleDTO.candidateUserId();
        Long interviewerUserId = scheduleDTO.interviewerUserId();

        if (!userId.equals(candidateUserId) && !userId.equals(interviewerUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_PARTICIPANT);
        }
        // 校验排期状态为 CONFIRMED、当前时间位于允许进入窗口，并从排期读取 TEXT/VOICE 类型。
        if (scheduleDTO.status() != InterviewScheduleStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_CONFIRMED);
        }
        if (scheduleDTO.interviewTime().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_NOT_SET);
        }
        InterviewType interviewType = scheduleDTO.interviewType();
        // TODO ③ 按 scheduleId 查询或幂等创建统一 interview_sessions；VOICE 仅额外创建 voice_interview_sessions 扩展记录。
        InterviewSession session = baseMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<InterviewSession>lambdaQuery()
                        .select(
                                InterviewSession::getId, InterviewSession::getEnterpriseId,
                                InterviewSession::getUserId, InterviewSession::getScheduleId,
                                InterviewSession::getSessionType, InterviewSession::getAttemptNo,
                                InterviewSession::getStatus, InterviewSession::getLastEventSequence
                        )
                        .eq(InterviewSession::getScheduleId, scheduleId));
        if (session == null) {
            // 首次进入：创建统一会话；VOICE 仅额外创建语音扩展记录。
            session = InterviewSession.builder()
                    .enterpriseId(scheduleDTO.enterpriseId())
                    .userId(candidateUserId)
                    .scheduleId(scheduleId)
                    .sessionType(interviewType)
                    .attemptNo((short) 1)
                    .idempotencyKey(idempotencyKey)
                    .status(InterviewSessionStatus.CREATED)
                    .build();
            save(session);
            if (interviewType == InterviewType.VOICE) {
                voiceInterviewSessionMapper.insert(
                        VoiceInterviewSession.builder()
                                .interviewSessionId(session.getId())
                                .build());
            }
            // TODO ④ 首次进入时通过跨模块 API 原子推进排期 CONFIRMED→IN_PROGRESS；断线重连（session != null）跳过推进。
            interviewScheduleCommandApi.startSchedule(scheduleId, scheduleDTO.enterpriseId());
        }
        // 生成绑定 userId、sessionId、参与者角色且 60 秒内只能消费一次的 connectionToken。
        String connectionToken = UUID.randomUUID().toString().replace("-", "");
        String role = userId.equals(candidateUserId) ? "CANDIDATE" : "INTERVIEWER";
        InterviewConnectionContext context = InterviewConnectionContext.builder()
                .userId(userId)
                .sessionId(session.getId())
                .scheduleId(scheduleId)
                .enterpriseId(scheduleDTO.enterpriseId())
                .role(role)
                .build();
        String key = InterviewConnectionKeyConstant.getConnectionKey(connectionToken);
        stringRedisTemplate.opsForValue().set(
                key, toJson(context), CONNECTION_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        // 返回统一 wsUrl、会话类型、尝试次数、状态和最后事件序号；文本/语音统一走同一入口。
        return new InterviewJoinTokenVO(
                session.getId(),
                scheduleId,
                session.getSessionType(),
                session.getAttemptNo(),
                session.getStatus(),
                connectionToken,
                "/ws/v1/interview-sessions/" + session.getId(),
                CONNECTION_TOKEN_TTL_SECONDS,
                session.getLastEventSequence());
    }

    @Override
    public InterviewConnectionContext consumeConnectionToken(String token) {
        // 原子比较凭证内容后删除，保证凭证只能被消费一次。
        String key = InterviewConnectionKeyConstant.getConnectionKey(token);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        Long consumed = stringRedisTemplate.execute(
                RedisScripts.CONSUME_ONCE, Collections.singletonList(key), json);
        if (consumed == null || consumed != 1L) {
            return null;
        }
        return fromJson(json);
    }

    @Override
    public InterviewSessionVO getSession(Long sessionId) {
        // 纯 CRUD
        Long userId = AuthContext.getRequiredUserId();

        // 查询统一会话，再校验当前用户是候选人或当前企业参与者。
        InterviewSession session = lambdaQuery()
                .select(
                        InterviewSession::getId, InterviewSession::getEnterpriseId,
                        InterviewSession::getUserId, InterviewSession::getScheduleId,
                        InterviewSession::getAttemptNo, InterviewSession::getSessionType,
                        InterviewSession::getStatus, InterviewSession::getLastEventSequence,
                        InterviewSession::getStartedAt, InterviewSession::getEndedAt
                )
                .eq(InterviewSession::getId, sessionId)
                .one();
        Long enterpriseId = AuthContext.getEnterpriseId();
        if (session == null
                || (!userId.equals(session.getUserId())
                && !java.util.Objects.equals(enterpriseId, session.getEnterpriseId()))) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }

        // 语音会话按需补充扩展信息，文本会话返回 null。
        VoiceDetailsVO voiceDetails = null;
        if (session.getSessionType() == InterviewType.VOICE) {
            VoiceInterviewSession voiceSession = voiceInterviewSessionMapper.selectOne(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers
                            .lambdaQuery(VoiceInterviewSession.class)
                            .select(
                                    VoiceInterviewSession::getCurrentPhase,
                                    VoiceInterviewSession::getActualDurationSeconds
                            )
                            .eq(VoiceInterviewSession::getInterviewSessionId, sessionId)
            );
            if (voiceSession != null) {
                voiceDetails = new VoiceDetailsVO(
                        voiceSession.getCurrentPhase(),
                        voiceSession.getActualDurationSeconds()
                );
            }
        }

        return new InterviewSessionVO(
                session.getId(),
                session.getScheduleId(),
                session.getSessionType(),
                session.getAttemptNo(),
                session.getStatus(),
                session.getLastEventSequence(),
                voiceDetails,
                session.getStartedAt(),
                session.getEndedAt()
        );
    }

    @Override
    @Transactional
    public InterviewSessionEndVO endSession(Long sessionId, InterviewSessionEndReq req) {
        // 从 AuthContext 取得用户并查询会话，校验其为候选人或当前企业面试官。
        Long userId = AuthContext.getRequiredUserId();
        InterviewSession session = baseMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<InterviewSession>lambdaQuery()
                        .eq(InterviewSession::getId, sessionId));
        Long enterpriseId = AuthContext.getEnterpriseId();
        if (session == null
                || (!userId.equals(session.getUserId())
                && !java.util.Objects.equals(enterpriseId, session.getEnterpriseId()))) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
        // 已为 COMPLETED 时直接返回原结束结果，幂等重放不重复推进排期与报告。
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            return buildEndResult(session, getReportStatus(session));
        }
        // 校验结束请求的期望状态与当前会话状态一致。
        if (req.expectedStatus() != InterviewSessionStatus.IN_PROGRESS
                || session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_STATUS_INVALID);
        }
        // 使用 id + status 条件原子更新为 COMPLETED，写入结束时间和实际持续时间。
        OffsetDateTime now = OffsetDateTime.now();
        int updated = baseMapper.update(null,
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<InterviewSession>lambdaUpdate()
                        .eq(InterviewSession::getId, sessionId)
                        .eq(InterviewSession::getStatus, InterviewSessionStatus.IN_PROGRESS)
                        .set(InterviewSession::getStatus, InterviewSessionStatus.COMPLETED)
                        .set(InterviewSession::getEndedAt, now));
        if (updated == 0) {
            // 更新零行时重新读取，区分已经结束与状态竞争。
            InterviewSession latest = baseMapper.selectOne(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers
                            .<InterviewSession>lambdaQuery()
                            .eq(InterviewSession::getId, sessionId));
            if (latest != null
                    && latest.getStatus() == InterviewSessionStatus.COMPLETED) {
                return buildEndResult(latest, getReportStatus(latest));
            }
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_STATUS_INVALID);
        }
        // 条件推进排期 IN_PROGRESS→COMPLETED，面试终态由排期侧统一判定。
        interviewScheduleCommandApi.completeSchedule(
                session.getScheduleId(), session.getEnterpriseId());
        // 幂等创建 PENDING 报告记录，避免查询报告接口 404；报告 AI 生成由后续阶段接入。
        ensurePendingReport(session);
        return buildEndResult(completedSession(session, now), InterviewReportGenerationStatus.PENDING);
    }

    @Override
    public InterviewTimelinePageVO getTimeline(Long sessionId, Long afterSequence, Integer size) {
        // 复用统一会话查询完成候选人或企业参与者归属校验。
        getSession(sessionId);
        if (afterSequence == null || afterSequence < 0
                || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }

        // 多取一条判断 hasMore，避免额外执行 count 查询。
        List<InterviewTimelineEvent> rows = interviewTimelineEventMapper.selectPage(
                new Page<>(1, size + 1L, false),
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .lambdaQuery(InterviewTimelineEvent.class)
                        .select(
                                InterviewTimelineEvent::getEventId,
                                InterviewTimelineEvent::getSequenceNum,
                                InterviewTimelineEvent::getEventType,
                                InterviewTimelineEvent::getPayloadJson,
                                InterviewTimelineEvent::getOccurredAt
                        )
                        .eq(InterviewTimelineEvent::getSessionId, sessionId)
                        .gt(InterviewTimelineEvent::getSequenceNum, afterSequence)
                        .orderByAsc(InterviewTimelineEvent::getSequenceNum)
        ).getRecords();
        boolean hasMore = rows.size() > size;
        List<InterviewTimelineEventVO> events = rows.stream()
                .limit(size)
                .map(event -> new InterviewTimelineEventVO(
                        event.getEventId(),
                        event.getSequenceNum(),
                        event.getEventType(),
                        parsePayload(event.getPayloadJson()),
                        event.getOccurredAt()
                ))
                .toList();
        long lastSequence = events.isEmpty()
                ? afterSequence
                : events.getLast().sequence();
        return new InterviewTimelinePageVO(lastSequence, hasMore, events);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payloadJson) {
        return (Map<String, Object>) toBean(payloadJson, Map.class);
    }

    private String toJson(InterviewConnectionContext context) {
        return toJsonStr(context);
    }

    private InterviewConnectionContext fromJson(String json) {
        return toBean(json, InterviewConnectionContext.class);
    }

    /**
     * 以 COMPLETED 与结束时间补齐会话信息，供返回结束结果使用。
     */
    private InterviewSession completedSession(InterviewSession session, OffsetDateTime endedAt) {
        session.setStatus(InterviewSessionStatus.COMPLETED);
        session.setEndedAt(endedAt);
        return session;
    }

    /**
     * 查询会话对应的报告生成状态；不存在记录时视为 PENDING。
     */
    private InterviewReportGenerationStatus getReportStatus(InterviewSession session) {
        InterviewReport report = interviewReportMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<InterviewReport>lambdaQuery()
                        .select(InterviewReport::getGenerationStatus)
                        .eq(InterviewReport::getScheduleId, session.getScheduleId())
                        .eq(InterviewReport::getEnterpriseId, session.getEnterpriseId()));
        return report == null
                ? InterviewReportGenerationStatus.PENDING
                : report.getGenerationStatus();
    }

    /**
     * 幂等创建 PENDING 报告记录，保证报告查询接口可看到任务状态。
     */
    private void ensurePendingReport(InterviewSession session) {
        InterviewReport report = interviewReportMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<InterviewReport>lambdaQuery()
                        .select(InterviewReport::getId)
                        .eq(InterviewReport::getScheduleId, session.getScheduleId())
                        .eq(InterviewReport::getEnterpriseId, session.getEnterpriseId()));
        if (report != null) {
            return;
        }
        interviewReportMapper.insert(InterviewReport.builder()
                .enterpriseId(session.getEnterpriseId())
                .scheduleId(session.getScheduleId())
                .generationStatus(InterviewReportGenerationStatus.PENDING)
                .build());
    }

    /**
     * 组装会话结束结果 VO。
     */
    private InterviewSessionEndVO buildEndResult(InterviewSession session,
                                                 InterviewReportGenerationStatus reportStatus) {
        return new InterviewSessionEndVO(
                session.getId(),
                session.getSessionType(),
                session.getStatus(),
                durationSeconds(session),
                reportStatus,
                voiceEvaluationStatus(session),
                session.getEndedAt()
        );
    }

    /**
     * 从 startedAt/endedAt 计算实际持续秒数；缺失时按 0 处理。
     */
    private Integer durationSeconds(InterviewSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) {
            return 0;
        }
        return (int) Math.max(0,
                ChronoUnit.SECONDS.between(session.getStartedAt(), session.getEndedAt()));
    }

    /**
     * 语音评估状态：文本会话为空，语音会话存在语音扩展记录时返回 PENDING。
     */
    private VoiceEvaluationStatus voiceEvaluationStatus(InterviewSession session) {
        if (session.getSessionType() != InterviewType.VOICE) {
            return null;
        }
        long count = voiceInterviewSessionMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<VoiceInterviewSession>lambdaQuery()
                        .eq(VoiceInterviewSession::getInterviewSessionId, session.getId()));
        return count > 0 ? VoiceEvaluationStatus.PENDING : null;
    }
}
