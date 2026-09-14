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
 * <p>协作类已就位：领域事件 {@code InterviewAnswerSubmittedEvent}、
 * 监听器 {@code InterviewSessionEventListener#handleAnswerSubmitted}、
 * 追问题生成 {@code InterviewQuestionExecutionService#generateFollowUpQuestion}、
 * REST 端点 {@code POST /interview-sessions/{sessionId}/answers}。</p>
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class InterviewAnswerServiceImpl implements InterviewAnswerService {

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewAnswerMapper interviewAnswerMapper;
    private final InterviewTimelineEventMapper interviewTimelineEventMapper;
    /** 仅 FINISH 分支复用会话收尾语义；如不做自动结束可移除。 */
    private final InterviewSessionService interviewSessionService;
    /** 仅评分分支需要；选择「先落 null 评分」时本依赖无实际调用。 */
    private final InterviewQuestionAiApi interviewQuestionAiApi;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public InterviewAnswerSubmitVO submitAnswer(Long sessionId, String idempotencyKey,
                                                InterviewAnswerSubmitReq req) {
        // ==================== TODO(你来实现) U3-08 作答提交 ====================
        // 步骤 1 归属与状态校验：
        //   AuthContext.getRequiredUserId() 取当前候选人；校验会话存在、
        //   session.userId == 当前用户、status == IN_PROGRESS（非进行中提交作答应拒绝）。
        //   参照写法：InterviewSessionServiceImpl.readySession:540 的
        //   lambdaQuery select 指定列 + 归属校验 + 状态机校验。
        //   注意：不要改走 InterviewSessionService#getSession，它放行企业侧访问，
        //   而本用例只允许候选人本人提交。
        //
        // 步骤 2 定位当前待答题（防客户端伪造题目）：
        //   从 interview_timeline_events 取最新一条 eventType = 'question.completed'
        //   的事件，其 payloadJson.questionId 即当前待答题目 ID
        //   （与 InterviewSessionServiceImpl:326 getCurrentQuestion 同源）。
        //   再用该 ID 查 interview_answers 行，题目信息直接取自该行：
        //   questionIndex / questionText / parentMessageId / followUpDepth
        //   （不必从 payload 逐个解析，行里就是权威值）。
        //   校验：该行 user_answer 必须为空——非空说明此题已作答，
        //   抛 PARAM_VALID_ERROR（"当前无待答题目"），否则会造成重复作答。
        //   若不存在 question.completed 事件（题目尚未生成）同样按无待答题处理。
        //   参照写法：InterviewSessionServiceImpl:370 parsePayload。
        //
        // 步骤 3 幂等：
        //   idempotencyKey 非空时，先查 interview_answers
        //   （session_id + idempotency_key），命中直接重放既有结果
        //   （用既有行的 score / aiFeedback 组装 VO 返回，不重复触发追问）。
        //   参照写法：NotificationTxService.selectByIdIdempotencyKey。
        //
        // 步骤 4 落库（注意：是回填，不是插入）：
        //   题目行在出题时已由 InterviewQuestionTxService.recordQuestion 插入
        //   （user_answer 为空），本步用条件更新回填该行：
        //   条件 = id + session_id + user_answer IS NULL，
        //   写入 userAnswer / answeredAt = now / idempotencyKey / traceId。
        //   参照写法：InterviewSessionServiceImpl:581 readySession 的
        //   lambdaUpdate 条件更新 + 零行归因。
        //   并发兜底：更新零行时回查该行——若 user_answer 已有值，
        //   说明并发重复提交，按幂等重放既有结果返回，而不是报错。
        //   自动填充失效：traceId 需显式 set TraceUtil.getTraceId()。
        //
        // 步骤 5 评分（score / aiFeedback）：
        //   生成方式由你决策，三个选项：
        //   a) 同步调 AI 评分——阻塞作答响应，AI 慢时拖垮接口，不推荐先做；
        //   b) 先落 null（表 CHECK 允许 score 为空）——推荐先跑通闭环；
        //   c) 扩展 InterviewQuestionAiApi 增加专门的评分方法。
        //
        // 步骤 6 追问或结束判定：
        //   followUpDepth < 3（表 CHECK 上限）时发布
        //   InterviewAnswerSubmittedEvent，字段为
        //   sessionId / scheduleId / enterpriseId / answerId（本题作答行 ID）/
        //   questionIndex / followUpDepth / questionText / userAnswer / traceId，
        //   由监听器在事务提交后异步生成追问题。
        //   事件发布必须在事务内（监听器用 AFTER_COMMIT + @Async 消费，
        //   与首题 InterviewSessionReadyEvent 模式一致）。
        //   追问已到深度 3 时的分支由你决策：
        //   - 交由客户端或显式 end 接口结束（最简，followUpGenerated = false）；
        //   - 或复用 interviewSessionService.endSession 自动结束（需构造 InterviewSessionEndReq）。
        //   注意：interview_sessions.total_questions 目前没有任何代码写入，
        //   「题目数上限」当前没有可信来源，不建议据此判定 FINISH。
        //   附加：作答成功建议追加一条 timeline 事件（answer.completed），与
        //   question.completed 对称——是否需要由你按前端展示需求定。
        //
        // 步骤 7 组装并返回 InterviewAnswerSubmitVO：
        //   answerId / score（可 null）/ aiFeedback（可 null）/
        //   followUpGenerated（是否已触发追问）/ sessionEnded。
        // ====================================================================
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "U3-08 作答提交尚未实现");
    }
}
