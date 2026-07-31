package interview.api.aicore.dto;

import java.util.List;

/**
 * 人才画像 AI 结果。
 */
public record AiCandidateProfileResult(
        String summary,
        List<AiCandidateDimensionScore> dimensions,
        LlmConfigSnapshotDTO llmConfigSnapshot
) {
}
