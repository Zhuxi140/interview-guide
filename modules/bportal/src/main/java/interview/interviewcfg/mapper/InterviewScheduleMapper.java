package interview.interviewcfg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.enums.InterviewScheduleStatus;
import interview.interviewcfg.model.bo.InterviewScheduleQueryBO;
import interview.interviewcfg.model.entity.InterviewSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface InterviewScheduleMapper extends BaseMapper<InterviewSchedule> {

    /**
     * 分页查询排期及投递、岗位关联信息
     * @param page 分页对象
     * @param enterpriseId 企业 ID
     * @param candidateUserId 候选人用户 ID
     * @param status 排期状态
     * @param startTime 面试开始时间
     * @param endTime 面试结束时间
     * @param sort 排序字段
     * @param ascending 是否升序
     * @return 排期关联分页
     */
    IPage<InterviewScheduleQueryBO> pageSchedulesWithApplication(
            IPage<?> page,
            @Param("enterpriseId") Long enterpriseId,
            @Param("candidateUserId") Long candidateUserId,
            @Param("status") InterviewScheduleStatus status,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("sort") String sort,
            @Param("ascending") boolean ascending);

    /**
     * 查询指定企业中的排期关联信息
     * @param enterpriseId 企业 ID
     * @param scheduleId 排期 ID
     * @return 排期关联信息
     */
    InterviewScheduleQueryBO getScheduleWithApplication(
            @Param("enterpriseId") Long enterpriseId,
            @Param("scheduleId") Long scheduleId);

    /**
     * 按主键查询排期关联信息
     * @param scheduleId 排期 ID
     * @return 排期关联信息
     */
    InterviewScheduleQueryBO getScheduleWithApplicationById(
            @Param("scheduleId") Long scheduleId);

    /**
     * 批量查询排期关联信息
     * @param scheduleIds 排期 ID 列表
     * @return 排期关联信息列表
     */
    List<InterviewScheduleQueryBO> listSchedulesWithApplication(
            @Param("scheduleIds") List<Long> scheduleIds);

    /**
     * 查询候选人拥有的有效排期 ID
     * @param candidateUserId 候选人用户 ID
     * @return 排期 ID 列表
     */
    List<Long> listScheduleIdsByCandidate(
            @Param("candidateUserId") Long candidateUserId);
}
