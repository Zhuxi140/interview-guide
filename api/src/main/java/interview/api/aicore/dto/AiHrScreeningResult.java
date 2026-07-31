package interview.api.aicore.dto;

import java.util.List;

/**
 * HR 人岗匹配 AI 结果。
 */
public record AiHrScreeningResult(
        Integer overallMatchScore,
        List<AiDimensionMatch> dimensionMatches,
        LlmConfigSnapshotDTO llmConfigSnapshot
) {
}
