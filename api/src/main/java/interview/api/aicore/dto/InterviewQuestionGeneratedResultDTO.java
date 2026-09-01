package interview.api.aicore.dto;

/**
 * AI 题目生成结果 DTO
 */
public record InterviewQuestionGeneratedResultDTO(
        String content,
        String assessmentPoint,
        String difficulty,
        String questionKind,
        String rawModelResponse
) {
}
