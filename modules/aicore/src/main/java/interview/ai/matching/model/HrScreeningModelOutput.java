package interview.ai.matching.model;

import interview.api.aicore.dto.AiDimensionMatch;

import java.util.List;

/**
 * LLM HR 人岗匹配结构化输出。
 */
public record HrScreeningModelOutput(
        Integer overallMatchScore,
        List<AiDimensionMatch> dimensionMatches
) {
}
