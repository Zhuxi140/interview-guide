package interview.textinterview.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.mapper.InterviewTimelineEventMapper;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.entity.InterviewTimelineEvent;
import interview.textinterview.model.req.InterviewAnswerSubmitReq;
import interview.textinterview.model.req.InterviewSessionCreateReq;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.*;
import interview.textinterview.service.InterviewSessionService;
import interview.voiceinterview.mapper.VoiceInterviewSessionMapper;
import interview.voiceinterview.model.entity.VoiceInterviewSession;
import interview.voiceinterview.model.vo.VoiceDetailsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl
        extends ServiceImpl<InterviewSessionMapper, InterviewSession>
        implements InterviewSessionService {

    private final VoiceInterviewSessionMapper voiceInterviewSessionMapper;
    private final InterviewTimelineEventMapper interviewTimelineEventMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InterviewJoinTokenVO generateJoinToken(Long scheduleId, String idempotencyKey) {
        // TODO ① 从 AuthContext 获取用户，并通过 InterviewScheduleQueryApi 校验其为候选人或当前企业面试官。
        // TODO ② 校验排期状态为 CONFIRMED、当前时间位于允许进入窗口，并从排期读取 TEXT/VOICE 类型。
        // TODO ③ 按 scheduleId 查询或幂等创建统一 interview_sessions；VOICE 仅额外创建 voice_interview_sessions 扩展记录。
        // TODO ④ 首次进入时通过跨模块 API 原子推进排期 CONFIRMED→IN_PROGRESS，断线重连复用原会话。
        // TODO ⑤ 生成绑定 userId、sessionId、参与者角色且 60 秒内只能消费一次的 connectionToken。
        // TODO ⑥ 返回统一 wsUrl、会话类型、尝试次数、状态和最后事件序号；不得按文本/语音再暴露两套入口。
        return null;
    }

    @Override
    @Transactional
    public InterviewSessionCreateVO createSession(InterviewSessionCreateReq req) {
        // TODO ① 从 AuthContext 取得候选人 userId，并将请求头 Idempotency-Key 传入 Service；当前方法签名需先补齐。
        // TODO ② 通过跨模块 ScheduleApi 查询排期，校验属于当前候选人、类型为 TEXT、状态为 CONFIRMED 且处于允许开始的时间窗口。
        // TODO ③ 按 scheduleId + userId + 幂等键查询已有会话；同请求重放返回原 session/首题，不同请求复用键返回冲突。
        // TODO ④ 通过跨模块 TemplateApi 读取模板阶段和 phaseConfigs 快照，计算 totalQuestions 并校验每个阶段均有可用策略。
        // TODO ⑤ 将当前长事务拆为短事务预占、事务外 AI 调用、短事务确认；禁止持有数据库事务等待模型生成首题。
        // TODO ⑥ 原子创建会话并保存首题待答上下文，初始 currentQuestionIndex/sessionVersion 和状态设为 IN_PROGRESS。
        // TODO ⑦ 通过 ScheduleApi 条件推进 CONFIRMED→IN_PROGRESS；任一步失败需补偿或由可靠消息收敛，禁止跨模块直写排期表。
        // TODO ⑧ 组装 sessionId、sessionVersion、首题 id/index/text 和 IN_PROGRESS；实体/表需补齐 version、幂等键及当前题持久化模型。
        // TODO ⑨ Phase 4/8 扩展：生成首题前校验余额与 KYC，记录一次 AI 调用及 Token 使用事实。
        return null;
    }

    @Override
    public InterviewSessionVO getSession(Long sessionId) {
        // 纯 CRUD
        Long userId = AuthContext.getRequiredUserId();

        // 查询统一会话，再校验当前用户是候选人或当前企业参与者。
        InterviewSession session = lambdaQuery()
                .select(
                        InterviewSession::getId,
                        InterviewSession::getEnterpriseId,
                        InterviewSession::getUserId,
                        InterviewSession::getScheduleId,
                        InterviewSession::getAttemptNo,
                        InterviewSession::getSessionType,
                        InterviewSession::getStatus,
                        InterviewSession::getLastEventSequence,
                        InterviewSession::getStartedAt,
                        InterviewSession::getEndedAt
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
        if ("VOICE".equals(session.getSessionType())) {
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
                interview.common.enums.InterviewType.valueOf(session.getSessionType()),
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
    public InterviewAnswerSubmitVO submitAnswer(Long sessionId, InterviewAnswerSubmitReq req) {
        // TODO ① 从 AuthContext 取得 userId，并将 Idempotency-Key 传入 Service；校验答案非空、长度上限，Phase 8 接入敏感词检查。
        // TODO ② 查询候选人本人的 IN_PROGRESS 会话，校验 questionId、expectedQuestionIndex、expectedSessionVersion 与当前待答题完全一致。
        // TODO ③ 按 sessionId + 幂等键查询答案；相同请求重放返回原结果，不同题目/答案复用幂等键返回冲突。
        // TODO ④ 使用短事务和条件更新预占当前题，写入待评分答案；更新零行时返回题目已提交或会话版本冲突。
        // TODO ⑤ 在事务外调用 AI 完成评分、反馈和追问决策；记录独立 attempt/Token，模型失败不得重复插入用户答案。
        // TODO ⑥ 需要追问时校验 parentMessageId 链路且 followUpDepth<3；否则按模板阶段和题量策略生成下一道主问题。
        // TODO ⑦ 在新的短事务写回 score/aiFeedback，持久化追问或下一题，并原子推进 currentQuestionIndex 与 sessionVersion。
        // TODO ⑧ 达到总题数且无追问时幂等结束会话、推进排期为 COMPLETED，并可靠创建 PENDING 报告生成任务。
        // TODO ⑨ 返回 answerId、questionIndex、评分反馈、nextQuestion、isLastQuestion 和新 sessionVersion。
        // TODO ⑩ 实现前需补齐 session.version、答案幂等/处理状态及当前待答题模型，避免消息重试再次消耗 AI Token。
        return null;
    }

    @Override
    public IPage<InterviewHistoryItemVO> getHistory(Long sessionId, Long cursor, Integer size) {
        // TODO ① 从 AuthContext 取得 userId，校验会话存在且属于当前候选人，限制 size 最大值。
        // TODO ② 将 cursor 解释为稳定的 answerId/复合游标，按 sessionId + id 单向查询 size+1 条，禁止 offset 模拟游标。
        // TODO ③ 按 parentMessageId 组装主问题与追问树，保持 questionIndex 和 followUpDepth 顺序，最大只展开三层。
        // TODO ④ 映射 questionText、userAnswer、score、aiFeedback、answeredAt 和 followUps。
        // TODO ⑤ 根据多取的一条记录计算 hasMore/nextCursor；当前 IPage 返回类型与文档游标响应不一致，需先统一接口模型。
        return null;
    }

    @Override
    @Transactional
    public InterviewSessionEndVO endSession(Long sessionId, InterviewSessionEndReq req) {
        // TODO ① 从 AuthContext 取得 userId，查询统一会话并校验其为候选人或当前企业面试官。
        // TODO ② 已为 COMPLETED 时返回原结束结果；当前状态必须与 expectedStatus=IN_PROGRESS 一致。
        // TODO ③ 校验当前没有处于评分中的答案；按业务规则决定允许提前结束还是必须完成最低题数。
        // TODO ④ 使用 id + status=IN_PROGRESS 条件原子更新为 COMPLETED，写入 endedAt 和实际持续时间。
        // TODO ⑤ 更新零行时区分会话不存在、已经结束和状态竞争，禁止并发事件覆盖结束状态。
        // TODO ⑥ 通过 ScheduleApi 条件推进 IN_PROGRESS→COMPLETED，并写入对应流转日志。
        // TODO ⑦ 同一业务事务创建 generationStatus=PENDING 的报告任务和可靠消息，幂等键绑定 sessionId。
        // TODO ⑧ 事务提交后异步生成报告；失败由消息重试收敛，查询接口始终可以看到 PENDING/PROCESSING/FAILED。
        // TODO ⑨ 返回统一的 sessionType、COMPLETED、持续时间、报告状态及可选语音评估状态。
        return null;
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
        try {
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }
}
