package interview.textinterview.event;

/**
 * 面试作答已落库领域事件（触发异步生成追问题）。
 *
 * <p>与 {@link InterviewSessionReadyEvent} 对称：由作答提交用例在事务内发布，
 * 监听器在事务提交后异步消费，把 AI 出题这段长耗时调用移出作答请求链路。</p>
 *
 * @param sessionId     会话 ID
 * @param scheduleId    排期 ID（出题需读取排期模板快照）
 * @param enterpriseId  企业 ID
 * @param answerId      本次作答记录 ID，同时作为追问题的 parentAnswerId
 * @param questionIndex 被作答题目序号（追问题序号为该值 + 1）
 * @param followUpDepth 被作答题目的追问深度（追问题深度为该值 + 1）
 * @param questionText  被作答题目正文，作为追问生成的上下文
 * @param userAnswer    候选人作答正文，作为追问生成的上下文
 * @param traceId       链路追踪 ID
 */
public record InterviewAnswerSubmittedEvent(
        Long sessionId,
        Long scheduleId,
        Long enterpriseId,
        Long answerId,
        int questionIndex,
        int followUpDepth,
        String questionText,
        String userAnswer,
        String traceId
) {
}
