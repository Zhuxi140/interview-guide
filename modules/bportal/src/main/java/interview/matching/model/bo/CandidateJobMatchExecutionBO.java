package interview.matching.model.bo;

/**
 * 候选人岗位适配预测执行上下文。
 */
public record CandidateJobMatchExecutionBO(
        Long analysisId,
        Long applicationId,
        Long candidateProfileId,
        String jobSnapshot,
        Integer attemptCount
) {
}
