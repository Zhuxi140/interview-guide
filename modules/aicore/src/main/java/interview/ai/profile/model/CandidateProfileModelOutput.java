package interview.ai.profile.model;

import interview.api.aicore.dto.AiCandidateDimensionScore;

import java.util.List;

/**
 * LLM 人才画像结构化输出。
 */
public record CandidateProfileModelOutput(
        String summary,
        List<AiCandidateDimensionScore> dimensions
) {
}
