package interview.textinterview.service;

import interview.textinterview.model.req.InterviewDecisionReq;
import interview.textinterview.model.req.InterviewScheduleCancelReq;
import interview.textinterview.model.vo.InterviewDecisionVO;
import interview.textinterview.model.vo.InterviewScheduleUpdateVO;

/**
 * 候选人面试邀请决策服务。
 */
public interface CandidateInterviewDecisionService {

    /**
     * 确认面试邀请
     * @param scheduleId 排期ID
     * @param req 确认请求
     * @return 确认结果
     */
    InterviewDecisionVO confirmSchedule(Long scheduleId, InterviewDecisionReq req);

    /**
     * 拒绝面试邀请
     * @param scheduleId 排期ID
     * @param req 拒绝请求
     * @return 拒绝结果
     */
    InterviewDecisionVO declineSchedule(Long scheduleId, InterviewDecisionReq req);

    /**
     * 取消已确认但尚未开始的面试
     * @param scheduleId 排期ID
     * @param req 取消请求
     * @return 取消结果
     */
    InterviewScheduleUpdateVO cancelSchedule(Long scheduleId, InterviewScheduleCancelReq req);
}
