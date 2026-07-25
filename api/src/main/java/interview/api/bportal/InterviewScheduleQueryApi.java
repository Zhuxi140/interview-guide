package interview.api.bportal;

import interview.api.bportal.dto.InterviewSchedulePageDTO;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.InterviewScheduleStatus;

import java.util.List;

/**
 * 面试排期只读 API。
 */
public interface InterviewScheduleQueryApi {

    /**
     * 分页查询候选人本人的面试排期
     * @param candidateUserId 候选人用户 ID
     * @param page 页码
     * @param size 每页条数
     * @param status 排期状态
     * @param sort 排序字段
     * @param order 排序方向
     * @return 排期分页数据
     */
    InterviewSchedulePageDTO pageCandidateSchedules(Long candidateUserId,
                                                    Integer page,
                                                    Integer size,
                                                    InterviewScheduleStatus status,
                                                    String sort,
                                                    String order);

    /**
     * 查询排期及其投递关联信息
     * @param scheduleId 排期 ID
     * @return 排期信息，不存在时返回 null
     */
    InterviewScheduleQueryDTO getSchedule(Long scheduleId);

    /**
     * 批量查询排期及其投递关联信息
     * @param scheduleIds 排期 ID 列表
     * @return 排期信息列表
     */
    List<InterviewScheduleQueryDTO> listSchedules(List<Long> scheduleIds);

    /**
     * 查询候选人拥有的有效排期 ID
     * @param candidateUserId 候选人用户 ID
     * @return 排期 ID 列表
     */
    List<Long> listScheduleIdsByCandidate(Long candidateUserId);
}
