package interview.interviewcfg.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.bportal.InterviewFlowStatusApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewFlowStatusEnum;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.exception.BusinessException;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.entity.InterviewSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 面试流程状态查询 API 实现。
 */
@Service
@RequiredArgsConstructor
public class InterviewFlowStatusApiImpl implements InterviewFlowStatusApi {

    private final InterviewScheduleMapper interviewScheduleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewFlowStatusEnum getInterviewFlowStatus(Long applicationId, Long enterpriseId) {
        // 查询该投递下的全部排期（按创建时间升序，首条即为最早轮次）。
        List<InterviewSchedule> schedules = interviewScheduleMapper.selectList(
                Wrappers.<InterviewSchedule>lambdaQuery()
                        .eq(InterviewSchedule::getApplicationId, applicationId)
                        .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                        .orderByAsc(InterviewSchedule::getCreatedAt));

        // 无排期：尚未进入面试阶段。
        if (schedules.isEmpty()) {
            return InterviewFlowStatusEnum.NO_SCHEDULE;
        }

        // 任一排期已被终结（拒绝/取消/未到场）：面试流程淘汰终止。
        boolean anyTerminal = schedules.stream()
                .anyMatch(schedule -> schedule.getStatus() == InterviewScheduleStatus.DECLINED
                        || schedule.getStatus() == InterviewScheduleStatus.CANCELLED
                        || schedule.getStatus() == InterviewScheduleStatus.NO_SHOW);
        if (anyTerminal) {
            return InterviewFlowStatusEnum.REJECTED_TERMINAL;
        }

        // 全部轮次排期均已完成，且已创建的轮次覆盖模板全部阶段：面试流程完成。
        int totalRounds = schedules.size();
        long completedRounds = schedules.stream()
                .filter(schedule -> schedule.getStatus() == InterviewScheduleStatus.COMPLETED)
                .count();
        int templateStageCount = readTemplateSnapshot(
                schedules.getFirst().getTemplateSnapshotJson()).stages().size();
        if (completedRounds == totalRounds && totalRounds >= templateStageCount) {
            return InterviewFlowStatusEnum.INTERVIEW_COMPLETED;
        }

        // 仍有待确认/已确认/进行中的排期，或轮次尚未排满。
        return InterviewFlowStatusEnum.IN_PROGRESS;
    }

    private InterviewTemplateSnapshot readTemplateSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, InterviewTemplateSnapshot.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR, e.getMessage());
        }
    }
}
