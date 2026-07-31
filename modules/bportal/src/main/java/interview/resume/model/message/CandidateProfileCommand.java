package interview.resume.model.message;

/**
 * 人才画像生成消息载荷。
 */
public record CandidateProfileCommand(
        Long resumeId,
        Long candidateId
) {
}
