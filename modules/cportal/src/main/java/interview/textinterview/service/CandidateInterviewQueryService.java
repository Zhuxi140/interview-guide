package interview.textinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.enums.InterviewScheduleStatus;
import interview.textinterview.model.vo.InterviewScheduleCandidateListItemVO;

/**
 * 候选人面试只读查询服务。
 */
public interface CandidateInterviewQueryService {

    /**
     * 分页查询候选人本人的面试排期
     * @param page 页码
     * @param size 每页条数
     * @param status 排期状态
     * @param sort 排序字段
     * @param order 排序方向
     * @return 排期分页
     */
    IPage<InterviewScheduleCandidateListItemVO> pageMySchedules(Integer page,
                                                                Integer size,
                                                                InterviewScheduleStatus status,
                                                                String sort,
                                                                String order);
}
