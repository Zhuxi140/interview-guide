package interview.textinterview.service;

import interview.api.aicore.InterviewQuestionAiApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.textinterview.mapper.InterviewAnswerMapper;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.mapper.InterviewTimelineEventMapper;
import interview.textinterview.model.req.InterviewAnswerSubmitReq;
import interview.textinterview.model.vo.InterviewAnswerSubmitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 候选人作答提交服务实现（U3-08）。
 *
 * <p>方法体为分步 TODO 骨架，由开发者按步骤补全实现；
 * 各步骤括号内标注了项目内可直接参照的既有写法。</p>
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class InterviewAnswerServiceImpl implements InterviewAnswerService {

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewAnswerMapper interviewAnswerMapper;
    private final InterviewTimelineEventMapper interviewTimelineEventMapper;
    private final InterviewQuestionAiApi interviewQuestionAiApi;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public InterviewAnswerSubmitVO submitAnswer(Long sessionId, InterviewAnswerSubmitReq req) {
        // ==================== TODO(你来实现) U3-08 作答提交 ====================
        // 步骤 1 归属与状态校验：
        //   AuthContext.getRequiredUserId() 取当前候选人；校验会话存在、
        //   session.userId == 当前用户、status == IN_PROGRESS（非进行中提交作答应拒绝）。
        //   参照写法：InterviewSessionServiceImpl.readySession:541 的
        //   lambdaQuery select 指定列 + 归属校验 + 状态机校验。
        //
        // 步骤 2 定位当前待答题（防客户端伪造题目）：
        //   复用 getCurrentQuestion 的语义——从 interview_timeline_events 取最新一条
        //   eventType = 'question.completed' 的事件，解析 payloadJson 得到
        //   questionIndex / questionText / parentMessageId / followUpDepth。
        //   参照写法：InterviewSessionServiceImpl:326 getCurrentQuestion 与 :370 parsePayload。
        //   若已无待答题（或该题已有作答）→ 抛 PARAM_VALID_ERROR("当前无待答题目")。
        //
        // 步骤 3 幂等：
        //   req.idempotencyKey 非空时，先查 interview_answers（session_id + idempotency_key），
        //   命中直接重放既有结果（组装 VO 返回，不重复落库、不重复触发追问）。
        //   参照写法：NotificationTxService.selectByIdempotencyKey。
        //
        // 步骤 4 落库：
        //   insert InterviewAnswer：questionIndex / questionText / parentMessageId /
        //   followUpDepth / userAnswer / answeredAt = now / traceId = TraceUtil.getTraceId()
        //   / idempotencyKey。
        //   ⚠️ 实体 InterviewAnswer 目前缺 idempotencyKey 字段（表 DDL 已有该列）——先补字段。
        //   并发兜底：catch DuplicateKeyException 后回查复用既有记录，
        //   参照写法：NotificationTxService.insertInApp 的 try/catch 模式。
        //   ⚠️ 请确认 DDL 是否已有 (session_id, idempotency_key) 部分唯一索引；
        //   若无，在 phase3-tables-init.sql 补一个（写法参照 uk_notifications_idem）。
        //
        // 步骤 5 评分（score / aiFeedback）：
        //   生成方式由你决策，三个选项：
        //   a) 同步调 AI 评分——阻塞作答响应，AI 慢时拖垮接口，不推荐先做；
        //   b) 先落 score = null（表 CHECK 允许），评分异步/后续迭代补——推荐先跑通闭环；
        //   c) 扩展 InterviewQuestionAiApi 增加专门的评分方法。
        //
        // 步骤 6 追问或结束判定：
        //   followUpDepth < 3（表 CHECK 上限）且需要追问时：发布
        //   InterviewAnswerSubmittedEvent（新事件 record，字段参照
        //   InterviewSessionReadyEvent：sessionId / enterpriseId / scheduleId / answerId），
        //   由 InterviewQuestionExecutionService 仿 generateFirstQuestion 生成追问题
        //   （生成请求里 parentAnswerId = 本次作答 id、followUpDepth + 1）。
        //   判定会话结束（如达到题目数上限）则复用 endSession 的收尾语义。
        //   事件发布必须在事务提交语义内（Spring 事件默认同事务同步处理，与首题模式一致）。
        //   附加：作答成功建议追加一条 timeline 事件（answer.completed），与
        //   question.completed 对称——是否需要由你按前端展示需求定。
        //
        // 步骤 7 组装并返回 InterviewAnswerSubmitVO：
        //   answerId / score（可 null）/ followUpGenerated（是否已触发追问）/ sessionEnded。
        // ====================================================================
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "U3-08 作答提交尚未实现");
    }
}
