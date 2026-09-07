package interview.textinterview.model.command;

/**
 * AI 面试题短事务写入命令。
 *
 * @param sessionId 会话 ID
 * @param enterpriseId 企业 ID
 * @param questionIndex 题目序号
 * @param parentAnswerId 父题目或上一题作答 ID
 * @param followUpDepth 追问深度
 * @param content 题目内容
 * @param questionKind 题目类型
 * @param assessmentPoint 考察点
 * @param difficulty 难度
 */
public record InterviewQuestionRecordCommand(
        Long sessionId,
        Long enterpriseId,
        int questionIndex,
        Long parentAnswerId,
        int followUpDepth,
        String content,
        String questionKind,
        String assessmentPoint,
        String difficulty
) {
}
