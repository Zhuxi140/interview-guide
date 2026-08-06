package interview.interviewcfg.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewSchedulePageDTO;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.exception.BusinessException;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.bo.InterviewScheduleQueryBO;
import interview.interviewcfg.model.entity.InterviewSchedule;
import interview.interviewcfg.service.InterviewScheduleService;
import interview.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 面试排期只读 API 实现。
 */
@Service
@RequiredArgsConstructor
public class InterviewScheduleQueryApiImpl implements InterviewScheduleQueryApi {

    private final InterviewScheduleMapper interviewScheduleMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final InterviewScheduleService interviewScheduleService;
    private final UserApi userApi;
    private final JobService jobService;

    @Override
    public InterviewSchedulePageDTO pageCandidateSchedules(Long candidateUserId, Integer page,
                                                           Integer size, InterviewScheduleStatus status,
                                                           String sort, String order) {
        // 校验跨模块查询参数，拒绝任意排序字段。
        validatePage(page, size);
        if (!"interviewTime".equals(sort)) {
            throw new BusinessException(ErrorCode.SORT_FIELD_INVALID);
        }
        if (!"asc".equalsIgnoreCase(order)
                && !"desc".equalsIgnoreCase(order)) {
            throw new BusinessException(ErrorCode.SORT_DIRECTION_INVALID);
        }

        // 在排期所属模块完成投递和岗位关联查询。
        IPage<InterviewScheduleQueryBO> boPage =
                interviewScheduleMapper.pageSchedulesWithApplication(
                        new Page<InterviewSchedule>(page, size),
                        null, candidateUserId,
                        status, null, null,
                        sort, "asc".equalsIgnoreCase(order)
                );
        List<InterviewScheduleQueryDTO> records = enrich(boPage.getRecords());
        return new InterviewSchedulePageDTO(
                boPage.getCurrent(), boPage.getSize(),
                boPage.getTotal(), boPage.getPages(),
                records
        );
    }

    @Override
    public InterviewScheduleQueryDTO getSchedule(Long scheduleId) {
        // 查询单条排期并补充跨模块展示名称。
        InterviewScheduleQueryBO schedule =
                interviewScheduleMapper.getScheduleWithApplicationById(scheduleId);
        if (schedule == null) {
            return null;
        }
        return enrich(List.of(schedule)).getFirst();
    }

    @Override
    public List<InterviewScheduleQueryDTO> listSchedules(List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }
        // 批量查询关联信息，避免报告分页出现逐条跨模块调用。
        List<Long> distinctIds = scheduleIds.stream().distinct().toList();
        return enrich(interviewScheduleMapper.listSchedulesWithApplication(distinctIds));
    }

    @Override
    public List<Long> listScheduleIdsByCandidate(Long candidateUserId) {
        // 候选人归属由投递记录确定。
        return interviewScheduleMapper.listScheduleIdsByCandidate(candidateUserId);
    }


    private List<InterviewScheduleQueryDTO> enrich(List<InterviewScheduleQueryBO> schedules) {
        if (schedules.isEmpty()) {
            return List.of();
        }
        List<Long> candidateIds = schedules.stream()
                .map(InterviewScheduleQueryBO::candidateUserId)
                .distinct()
                .toList();
        List<Long> enterpriseIds = schedules.stream()
                .map(InterviewScheduleQueryBO::enterpriseId)
                .distinct()
                .toList();
        List<Long> jobIds = schedules.stream()
                .map(InterviewScheduleQueryBO::jobId)
                .distinct()
                .toList();
        Map<Long, String> candidateNames = userApi.getUserNamesByIds(candidateIds);
        Map<Long, String> enterpriseNames =
                enterpriseValidationApi.getNameList(enterpriseIds);
        Map<Long, String> jobTitles = jobService.getJobTitlesByIds(jobIds);
        return schedules.stream()
                .map(schedule -> toDto(
                        schedule,
                        candidateNames.get(schedule.candidateUserId()),
                        enterpriseNames.get(schedule.enterpriseId()),
                        jobTitles.get(schedule.jobId())
                ))
                .toList();
    }

    private InterviewScheduleQueryDTO toDto(InterviewScheduleQueryBO schedule,
                                            String candidateName,
                                            String enterpriseName,
                                            String jobTitle) {
        return new InterviewScheduleQueryDTO(
                schedule.id(), schedule.enterpriseId(),
                schedule.applicationId(), schedule.candidateUserId(),
                schedule.jobId(), schedule.templateId(),
                schedule.roundNo(), schedule.phaseCode(),
                schedule.phaseName(), schedule.interviewerUserId(),
                candidateName, enterpriseName,
                jobTitle, schedule.interviewTime(),
                schedule.durationMinutes(), schedule.interviewType(),
                schedule.status(), schedule.statusReason(),
                schedule.version(), schedule.createdAt(),
                schedule.updatedAt()
        );
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

}
