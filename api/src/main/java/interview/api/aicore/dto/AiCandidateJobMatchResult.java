package interview.api.aicore.dto;

import java.util.List;

/**
 * 候选人岗位适配 AI 结果。
 */
public record AiCandidateJobMatchResult(
        Integer matchScore,
        Integer passProbability,
        List<String> strengths,
        List<String> gaps,
        LlmConfigSnapshotDTO llmConfigSnapshot
) {
}
