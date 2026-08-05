package interview.api.bportal;

import interview.common.enums.InterviewFlowStatusEnum;

/**
 * 面试流程状态查询 API。
 */
public interface InterviewFlowStatusApi {

    /**
     * 判定指定投递的面试流程状态
     * @param applicationId 投递 ID
     * @param enterpriseId 企业 ID
     * @return 面试流程状态
     */
    InterviewFlowStatusEnum getInterviewFlowStatus(Long applicationId, Long enterpriseId);
}
