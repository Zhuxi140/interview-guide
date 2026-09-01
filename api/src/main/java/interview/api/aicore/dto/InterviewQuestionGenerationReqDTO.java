package interview.api.aicore.dto;

import java.util.List;

/**
 * 面试出题请求 DTO（通用：支持开场首题、主干新题及动态追问）
 */
public record InterviewQuestionGenerationReqDTO(
        Long enterpriseId,
        String phaseCode,
        String promptOverride,
        List<String> focusPoints,
        String difficulty,
        String jobTitle,
        String jobRequirements,
        String candidateResumeSummary,
        List<Long> knowledgeBaseIds,
        String questionKind,
        Integer questionIndex,
        Long parentAnswerId,
        String previousQuestionText,
        String previousUserAnswer
) {
}
