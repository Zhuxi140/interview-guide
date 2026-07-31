package interview.ai.matching.model;

import java.util.List;

/**
 * LLM 候选人岗位适配结构化输出。
 */
public record CandidateJobMatchModelOutput(
        Integer matchScore,
        Integer passProbability,
        List<String> strengths,
        List<String> gaps
) {
}
