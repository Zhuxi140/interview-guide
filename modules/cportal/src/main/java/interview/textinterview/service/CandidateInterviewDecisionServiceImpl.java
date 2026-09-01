package interview.textinterview.service;

import interview.api.bportal.InterviewScheduleCommandApi;
import interview.api.bportal.dto.InterviewScheduleCommandResultDTO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.model.req.InterviewDecisionReq;
import interview.textinterview.model.req.InterviewScheduleCancelReq;
import interview.textinterview.model.vo.InterviewDecisionVO;
import interview.textinterview.model.vo.InterviewScheduleUpdateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 候选人面试邀请决策实现。
 */
@Service
@RequiredArgsConstructor
public class CandidateInterviewDecisionServiceImpl implements CandidateInterviewDecisionService {

    private final InterviewScheduleCommandApi interviewScheduleCommandApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewDecisionVO confirmSchedule(Long scheduleId, InterviewDecisionReq req) {
        // 校验候选人归属与期望状态，原子推进排期至 CONFIRMED。
        InterviewScheduleCommandResultDTO result = interviewScheduleCommandApi.candidateConfirm(
                scheduleId, AuthContext.getRequiredUserId(), req.expectedVersion());
        return toDecisionVO(result);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewDecisionVO declineSchedule(Long scheduleId, InterviewDecisionReq req) {
        // 候选人拒绝面试：排期终结并自动淘汰投递。
        InterviewScheduleCommandResultDTO result = interviewScheduleCommandApi.candidateDecline(
                scheduleId, AuthContext.getRequiredUserId(), req.expectedVersion(), req.reason());
        return toDecisionVO(result);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleUpdateVO cancelSchedule(Long scheduleId, InterviewScheduleCancelReq req) {
        // 候选人取消已确认面试：排期终结并自动淘汰投递。
        InterviewScheduleCommandResultDTO result = interviewScheduleCommandApi.candidateCancel(
                scheduleId, AuthContext.getRequiredUserId(), req.expectedVersion(), req.reason());
        return InterviewScheduleUpdateVO.builder()
                .id(result.scheduleId())
                .status(result.status())
                .version(result.version())
                .updatedAt(result.updatedAt())
                .build();
    }

    private InterviewDecisionVO toDecisionVO(InterviewScheduleCommandResultDTO result) {
        return new InterviewDecisionVO(
                result.scheduleId(), result.status(), result.version(), result.updatedAt());
    }
}
