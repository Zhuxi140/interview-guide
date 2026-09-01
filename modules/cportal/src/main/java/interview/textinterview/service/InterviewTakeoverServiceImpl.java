package interview.textinterview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewSessionStatus;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 面试官人工接管实现。
 *
 * <p>接管只以持久化的时间线事件和接管记录为事实来源；实时通道（AI_PAUSE/AI_RESUME 指令广播）
 * 依赖 WebSocket 与可靠消息基建，接入前仅预留扩展点。</p>
 */
@Service
@RequiredArgsConstructor
public class InterviewTakeoverServiceImpl extends ServiceImpl<InterviewTakeoverMapper, InterviewTakeover>
        implements InterviewTakeoverService {

    private static final String ACTOR_TYPE_INTERVIEWER = "INTERVIEWER";
    private static final String EVENT_TAKEOVER_STARTED = "TAKEOVER_STARTED";
    private static final String EVENT_AI_RESUMED = "AI_RESUMED";
    private static final String EVENT_TAKEOVER_ENDED = "TAKEOVER_ENDED";

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewScheduleQueryApi interviewScheduleQueryApi;
    private final InterviewTimelineEventMapper interviewTimelineEventMapper;
    private final InterviewSessionService interviewSessionService;

    @Override
    @Transactional
    public InterviewTakeoverStartVO startTakeover(Long enterpriseId,
                                                   Long sessionId,
                                                   String idempotencyKey,
                                                   InterviewTakeoverStartReq req) {
        // ① 从 AuthContext 取得面试官 userId；会话归属校验同时完成企业成员与租户范围校验。
        Long interviewerUserId = AuthContext.getRequiredUserId();
        InterviewSession session = loadSession(enterpriseId, sessionId);
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_STATUS_INVALID);
        }

        // ② 操作人必须是该排期面试官。
        InterviewScheduleQueryDTO schedule = interviewScheduleQueryApi.getSchedule(session.getScheduleId());
        if (schedule == null || !Objects.equals(schedule.interviewerUserId(), interviewerUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // ③ 按 sessionId + requestId 幂等：相同请求直接重放，复用键但内容不同则报幂等冲突。
        InterviewTakeover existing = baseMapper.selectOne(Wrappers.<InterviewTakeover>lambdaQuery()
                .select(InterviewTakeover::getId, InterviewTakeover::getSessionId,
                        InterviewTakeover::getStatus, InterviewTakeover::getReason,
                        InterviewTakeover::getStartedAt)
                .eq(InterviewTakeover::getSessionId, sessionId)
                .eq(InterviewTakeover::getRequestId, idempotencyKey));
        if (existing != null) {
            if (Objects.equals(existing.getReason(), req.reason())) {
                return buildStartVO(existing);
            }
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        // ④ 同一会话不得存在并发活跃接管；会话维度部分唯一索引兜底并发写入。
        boolean activeExists = baseMapper.selectCount(Wrappers.<InterviewTakeover>lambdaQuery()
                .eq(InterviewTakeover::getSessionId, sessionId)
                .eq(InterviewTakeover::getStatus, InterviewTakeoverStatus.ACTIVE)) > 0;
        if (activeExists) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }

        // ⑤ 短事务内插入 ACTIVE 接管记录，并写入持久化时间线事件。
        OffsetDateTime now = OffsetDateTime.now();
        InterviewTakeover takeover = InterviewTakeover.builder()
                .sessionId(sessionId)
                .enterpriseId(enterpriseId)
                .interviewerUserId(interviewerUserId)
                .requestId(idempotencyKey)
                .status(InterviewTakeoverStatus.ACTIVE)
                .reason(req.reason())
                .startedAt(now)
                .build();
        try {
            baseMapper.insert(takeover);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }
        appendTimelineEvent(takeover, EVENT_TAKEOVER_STARTED);

        // TODO [Realtime] 事务提交后向会话实时通道发布 AI_PAUSE 控制事件；发布失败由可靠消息
        //  重试并保持可回放事件为事实来源（WebSocket/可靠消息基建落地后接入）。
        return buildStartVO(takeover);
    }

    @Override
    @Transactional
    public InterviewTakeoverEndVO endTakeover(Long enterpriseId,
                                               Long sessionId,
                                               Long takeoverId,
                                               InterviewTakeoverEndReq req) {
        // ① 会话归属校验同时发布操作人必须为当前企业成员。
        loadSession(enterpriseId, sessionId);

        // ② 按租户、会话与接管记录查询，要求状态为 ACTIVE；已结束直接幂等重放。
        InterviewTakeover takeover = baseMapper.selectOne(Wrappers.<InterviewTakeover>lambdaQuery()
                .eq(InterviewTakeover::getId, takeoverId)
                .eq(InterviewTakeover::getEnterpriseId, enterpriseId)
                .eq(InterviewTakeover::getSessionId, sessionId));
        if (takeover == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TAKEOVER_NOT_FOUND);
        }
        if (takeover.getStatus() == InterviewTakeoverStatus.ENDED) {
            return buildEndVO(takeover, currentSessionStatus(sessionId));
        }
        if (takeover.getStatus() != InterviewTakeoverStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }

        // ③ takeoverId + status=ACTIVE 条件原子更新为 ENDED，区分并发结束与记录变更。
        OffsetDateTime endedAt = OffsetDateTime.now();
        int updated = baseMapper.update(null, Wrappers.<InterviewTakeover>lambdaUpdate()
                .eq(InterviewTakeover::getId, takeoverId)
                .eq(InterviewTakeover::getStatus, InterviewTakeoverStatus.ACTIVE)
                .set(InterviewTakeover::getStatus, InterviewTakeoverStatus.ENDED)
                .set(InterviewTakeover::getEndedAt, endedAt));
        if (updated == 0) {
            InterviewTakeover latest = baseMapper.selectById(takeoverId);
            if (latest != null && latest.getStatus() == InterviewTakeoverStatus.ENDED) {
                return buildEndVO(latest, currentSessionStatus(sessionId));
            }
            throw new BusinessException(ErrorCode.INTERVIEW_TAKEOVER_NOT_FOUND);
        }
        takeover.setStatus(InterviewTakeoverStatus.ENDED);
        takeover.setEndedAt(endedAt);

        // ④ 结束接管后的会话动作：恢复 AI 或结束会话。
        InterviewTakeoverEndAction action = req.action();
        InterviewSessionStatus sessionStatus;
        String eventType;
        if (action == InterviewTakeoverEndAction.RESUME_AI) {
            // 恢复 AI 前确认会话仍为 IN_PROGRESS，写入 AI_RESUME 时间线事件。
            requireInProgressSession(enterpriseId, sessionId);
            sessionStatus = InterviewSessionStatus.IN_PROGRESS;
            eventType = EVENT_AI_RESUMED;
        } else {
            // ⑤ END_SESSION：复用统一会话结束服务，幂等推进会话并可靠触发报告生成。
            InterviewSessionEndVO ended = interviewSessionService.endSession(
                    sessionId, new InterviewSessionEndReq(InterviewSessionStatus.IN_PROGRESS));
            sessionStatus = ended.status();
            eventType = EVENT_TAKEOVER_ENDED;
        }
        appendTimelineEvent(takeover, eventType);

        // TODO [Realtime] 事务提交后向会话实时通道发布 AI_RESUME / SESSION_END 指令；发布失败由可靠消息重试。
        return buildEndVO(takeover, sessionStatus);
    }

    private InterviewSession loadSession(Long enterpriseId, Long sessionId) {
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(enterpriseId, session.getEnterpriseId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
        return session;
    }

    private void requireInProgressSession(Long enterpriseId, Long sessionId) {
        InterviewSession session = loadSession(enterpriseId, sessionId);
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_STATUS_INVALID);
        }
    }

    private InterviewSessionStatus currentSessionStatus(Long sessionId) {
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        return session == null ? null : session.getStatus();
    }

    private void appendTimelineEvent(InterviewTakeover takeover, String eventType) {
        Long sequence = nextSequence(takeover.getSessionId());
        // 使用可容纳 null 的 JSONObject 构造载荷，避免实体主键未回填时序列化失败。
        JSONObject payload = new JSONObject();
        payload.set("takeoverId", takeover.getId());
        InterviewTimelineEvent event = InterviewTimelineEvent.builder()
                .sessionId(takeover.getSessionId())
                .enterpriseId(takeover.getEnterpriseId())
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .sequenceNum(sequence)
                .eventType(eventType)
                .actorType(ACTOR_TYPE_INTERVIEWER)
                .actorUserId(takeover.getInterviewerUserId())
                .payloadJson(JSONUtil.toJsonStr(payload))
                .occurredAt(OffsetDateTime.now())
                .build();
        interviewTimelineEventMapper.insert(event);
    }

    private Long nextSequence(Long sessionId) {
        List<InterviewTimelineEvent> rows = interviewTimelineEventMapper.selectPage(
                new Page<>(1, 1, false),
                Wrappers.<InterviewTimelineEvent>lambdaQuery()
                        .eq(InterviewTimelineEvent::getSessionId, sessionId)
                        .orderByDesc(InterviewTimelineEvent::getSequenceNum))
                .getRecords();
        return rows.isEmpty() ? 1L : rows.getFirst().getSequenceNum() + 1;
    }

    private InterviewTakeoverStartVO buildStartVO(InterviewTakeover takeover) {
        return new InterviewTakeoverStartVO(
                takeover.getId(), takeover.getSessionId(), takeover.getStatus(), takeover.getStartedAt());
    }

    private InterviewTakeoverEndVO buildEndVO(InterviewTakeover takeover, InterviewSessionStatus sessionStatus) {
        return new InterviewTakeoverEndVO(
                takeover.getId(), takeover.getStatus(), sessionStatus, takeover.getEndedAt());
    }
}