package interview.api.cportal.dto;

/**
 * 面试会话参与者轻量只读快照，供跨模块做会话归属与租户校验。
 *
 * @param sessionId       会话 ID
 * @param candidateUserId 会话归属的候选人用户 ID
 * @param enterpriseId    会话归属的企业租户 ID
 */
public record InterviewSessionParticipantDTO(
        Long sessionId,
        Long candidateUserId,
        Long enterpriseId
) {
}
