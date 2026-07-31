package interview.resume.model.bo;

/**
 * 人才画像执行上下文。
 */
public record CandidateProfileExecutionBO(
        Long profileId,
        Long resumeId,
        Long candidateId,
        String resumeText,
        Integer attemptCount
) {
}
