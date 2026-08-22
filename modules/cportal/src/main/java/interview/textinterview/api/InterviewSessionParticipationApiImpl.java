package interview.textinterview.api;

import interview.api.cportal.InterviewSessionParticipationApi;
import interview.api.cportal.dto.InterviewSessionParticipantDTO;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 面试会话参与者只读 API 实现。
 *
 * <p>会话表归 cportal 所有，本实现仅以单表 LambdaQuery 暴露归属信息，
 * 不做权限判定；调用方必须自行完成用户身份校验。</p>
 */
@Service
@RequiredArgsConstructor
public class InterviewSessionParticipationApiImpl implements InterviewSessionParticipationApi {

    private final InterviewSessionService interviewSessionService;

    @Override
    public InterviewSessionParticipantDTO getSessionParticipant(Long sessionId) {
        // 单表查询会话归属字段；逻辑删除条件由 MyBatis-Plus 自动追加。
        InterviewSession session = interviewSessionService.lambdaQuery()
                .select(
                        InterviewSession::getId,
                        InterviewSession::getUserId,
                        InterviewSession::getEnterpriseId
                )
                .eq(InterviewSession::getId, sessionId)
                .one();
        return session == null ? null : toParticipant(session);
    }

    @Override
    public List<InterviewSessionParticipantDTO> listSessionParticipants(Collection<Long> sessionIds) {
        // 批量 IN 查询补齐会话归属，供分页列表组装候选人信息使用。
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return interviewSessionService.lambdaQuery()
                .select(
                        InterviewSession::getId,
                        InterviewSession::getUserId,
                        InterviewSession::getEnterpriseId
                )
                .in(InterviewSession::getId, sessionIds)
                .list()
                .stream()
                .map(this::toParticipant)
                .toList();
    }

    @Override
    public List<Long> listSessionIdsByCandidate(Long candidateUserId) {
        // 按候选人反查其全部会话 ID，供企业侧按候选人过滤提交记录。
        if (candidateUserId == null) {
            return List.of();
        }
        return interviewSessionService.lambdaQuery()
                .select(InterviewSession::getId)
                .eq(InterviewSession::getUserId, candidateUserId)
                .list()
                .stream()
                .map(InterviewSession::getId)
                .toList();
    }

    private InterviewSessionParticipantDTO toParticipant(InterviewSession session) {
        return new InterviewSessionParticipantDTO(
                session.getId(),
                session.getUserId(),
                session.getEnterpriseId()
        );
    }
}
