package interview.matching.model.bo;

/**
 * HR AI 初筛执行上下文。
 */
public record ApplicationScreeningExecutionBO(
        Long screeningId,
        Long applicationId,
        Long candidateProfileId,
        String thresholdSnapshot,
        String jobSnapshot,
        Integer attemptCount
) {
}
