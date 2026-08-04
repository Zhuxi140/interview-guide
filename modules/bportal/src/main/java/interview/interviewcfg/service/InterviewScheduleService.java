package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.enums.InterviewScheduleStatus;
import interview.interviewcfg.model.entity.InterviewSchedule;
import interview.interviewcfg.model.req.*;
import interview.interviewcfg.model.vo.*;

public interface InterviewScheduleService extends IService<InterviewSchedule> {

    /**
     * HR 创建面试排期
     * @param enterpriseId 企业ID
     * @param applicationId 投递ID
     * @param req 创建请求
     * @param idempotencyKey 幂等键
     * @return 创建结果
     */
    InterviewScheduleCreateVO createSchedule(Long enterpriseId, Long applicationId, InterviewScheduleCreateReq req, String idempotencyKey);

    /**
     * 查询企业面试排期列表（分页）
     * @param enterpriseId 企业ID
     * @param page 页码
     * @param size 每页条数
     * @param status 状态筛选
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param sort 排序字段
     * @param order 排序方向
     * @return 排期分页
     */
    IPage<InterviewScheduleListItemVO> pageSchedules(Long enterpriseId, Integer page, Integer size,
                                                      InterviewScheduleStatus status,
                                                      String startTime, String endTime,
                                                      String sort, String order);

    /**
     * 查询排期详情
     * @param enterpriseId 企业ID
     * @param scheduleId 排期ID
     * @return 排期详情
     */
    InterviewScheduleDetailVO getScheduleDetail(Long enterpriseId, Long scheduleId);

    /**
     * HR 重新安排面试
     * @param enterpriseId 企业ID
     * @param scheduleId 排期ID
     * @param req 重新安排请求
     * @return 更新结果
     */
    InterviewScheduleUpdateVO reschedule(Long enterpriseId, Long scheduleId, InterviewScheduleRescheduleReq req);

    /**
     * HR 取消排期
     * @param enterpriseId 企业ID
     * @param scheduleId 排期ID
     * @param req 取消请求
     * @return 取消结果
     */
    InterviewScheduleUpdateVO cancelSchedule(Long enterpriseId, Long scheduleId, InterviewScheduleCancelReq req);

}
