package interview.api.bportal;

import interview.api.bportal.dto.InterviewScheduleCommandResultDTO;

/**
 * 面试排期写操作 API（候选人与系统自动化触发的状态推进）。
 */
public interface InterviewScheduleCommandApi {

    /**
     * 候选人确认面试：PENDING_CONFIRMATION → CONFIRMED
     * @param scheduleId 排期 ID
     * @param candidateUserId 候选人用户 ID
     * @param expectedVersion 期望版本号（乐观锁），null 表示不校验版本
     * @return 变更后的排期信息
     */
    InterviewScheduleCommandResultDTO candidateConfirm(Long scheduleId,
                                                       Long candidateUserId,
                                                       Integer expectedVersion);

    /**
     * 候选人拒绝面试：PENDING_CONFIRMATION → DECLINED，并自动回写投递为 REJECTED
     * @param scheduleId 排期 ID
     * @param candidateUserId 候选人用户 ID
     * @param expectedVersion 期望版本号（乐观锁），null 表示不校验版本
     * @param reason 拒绝原因
     * @return 变更后的排期信息
     */
    InterviewScheduleCommandResultDTO candidateDecline(Long scheduleId,
                                                       Long candidateUserId,
                                                       Integer expectedVersion,
                                                       String reason);

    /**
     * 候选人取消已确认的面试：CONFIRMED → CANCELLED，并自动回写投递为 REJECTED
     * @param scheduleId 排期 ID
     * @param candidateUserId 候选人用户 ID
     * @param expectedVersion 期望版本号（乐观锁），null 表示不校验版本
     * @param reason 取消原因
     * @return 变更后的排期信息
     */
    InterviewScheduleCommandResultDTO candidateCancel(Long scheduleId,
                                                      Long candidateUserId,
                                                      Integer expectedVersion,
                                                      String reason);

    /**
     * 首次进入面试时开始排期：CONFIRMED → IN_PROGRESS（幂等，已处于 IN_PROGRESS 时直接返回当前状态）
     * @param scheduleId 排期 ID
     * @param enterpriseId 企业 ID
     * @return 变更后的排期信息
     */
    InterviewScheduleCommandResultDTO startSchedule(Long scheduleId, Long enterpriseId);

    /**
     * 面试会话结束自动完成排期：IN_PROGRESS → COMPLETED
     * @param scheduleId 排期 ID
     * @param enterpriseId 企业 ID
     * @return 变更后的排期信息
     */
    InterviewScheduleCommandResultDTO completeSchedule(Long scheduleId, Long enterpriseId);

    /**
     * 标记候选人未到场：CONFIRMED → NO_SHOW，并自动回写投递为 REJECTED
     * @param scheduleId 排期 ID
     * @param enterpriseId 企业 ID
     * @param reason 未到场原因
     * @return 变更后的排期信息
     */
    InterviewScheduleCommandResultDTO markNoShow(Long scheduleId, Long enterpriseId, String reason);
}
