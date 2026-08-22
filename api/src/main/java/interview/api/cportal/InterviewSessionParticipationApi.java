package interview.api.cportal;

import interview.api.cportal.dto.InterviewSessionParticipantDTO;

import java.util.Collection;
import java.util.List;

/**
 * 面试会话参与者只读 API。
 *
 * <p>供 bportal 等模块在无法直接访问 cportal 会话表时，
 * 完成会话归属（候选人本人）与租户归属校验，避免跨模块直查表。</p>
 */
public interface InterviewSessionParticipationApi {

    /**
     * 查询会话的参与者快照
     * @param sessionId 会话 ID
     * @return 参与者快照；会话不存在或已删除时返回 null
     */
    InterviewSessionParticipantDTO getSessionParticipant(Long sessionId);

    /**
     * 批量查询会话的参与者快照
     * @param sessionIds 会话 ID 集合；为空时返回空列表
     * @return 参与者快照列表，仅包含仍存在的会话
     */
    List<InterviewSessionParticipantDTO> listSessionParticipants(Collection<Long> sessionIds);

    /**
     * 查询候选人的全部会话 ID
     * @param candidateUserId 候选人用户 ID
     * @return 会话 ID 列表；候选人无会话时返回空列表
     */
    List<Long> listSessionIdsByCandidate(Long candidateUserId);
}
