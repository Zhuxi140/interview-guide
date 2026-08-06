package interview.interviewcfg.api;

import interview.api.bportal.InterviewScheduleCommandApi;
import interview.api.bportal.JobValidationApi;
import interview.api.bportal.dto.InterviewScheduleCommandResultDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.bo.InterviewScheduleQueryBO;
import interview.interviewcfg.model.entity.InterviewSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 面试排期写操作 API 实现。
 */
@Service
@RequiredArgsConstructor
public class InterviewScheduleCommandApiImpl implements InterviewScheduleCommandApi {

    private final InterviewScheduleMapper interviewScheduleMapper;
    private final JobValidationApi jobValidationApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleCommandResultDTO candidateConfirm(Long scheduleId,
                                                              Long candidateUserId,
                                                              Integer expectedVersion) {
        // 候选人确认面试：仅允许 PENDING_CONFIRMATION → CONFIRMED。
        return transitionByCandidate(scheduleId, candidateUserId, expectedVersion,
                InterviewScheduleStatus.PENDING_CONFIRMATION, InterviewScheduleStatus.CONFIRMED, null);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleCommandResultDTO candidateDecline(Long scheduleId,
                                                              Long candidateUserId,
                                                              Integer expectedVersion,
                                                              String reason) {
        // 候选人拒绝面试：排期终结并自动淘汰投递。
        InterviewScheduleCommandResultDTO result = transitionByCandidate(scheduleId, candidateUserId,
                expectedVersion, InterviewScheduleStatus.PENDING_CONFIRMATION,
                InterviewScheduleStatus.DECLINED, reason);
        rejectApplicationBySchedule(scheduleId, reason);
        return result;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleCommandResultDTO candidateCancel(Long scheduleId,
                                                             Long candidateUserId,
                                                             Integer expectedVersion,
                                                             String reason) {
        // 候选人取消已确认面试：排期终结并自动淘汰投递。
        InterviewScheduleCommandResultDTO result = transitionByCandidate(scheduleId, candidateUserId,
                expectedVersion, InterviewScheduleStatus.CONFIRMED,
                InterviewScheduleStatus.CANCELLED, reason);
        rejectApplicationBySchedule(scheduleId, reason);
        return result;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleCommandResultDTO startSchedule(Long scheduleId, Long enterpriseId) {
        // 首次进入开始面试：排期须属于该企业；幂等容忍已处于 IN_PROGRESS 的并发进入。
        InterviewScheduleQueryBO schedule = getSchedule(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        if (schedule.status() == InterviewScheduleStatus.IN_PROGRESS) {
            return new InterviewScheduleCommandResultDTO(
                    schedule.id(), InterviewScheduleStatus.IN_PROGRESS,
                    schedule.version(), schedule.updatedAt());
        }
        return transitionByEnterprise(scheduleId, enterpriseId,
                InterviewScheduleStatus.CONFIRMED, InterviewScheduleStatus.IN_PROGRESS, null);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleCommandResultDTO completeSchedule(Long scheduleId, Long enterpriseId) {
        // 面试会话结束自动完成排期：IN_PROGRESS → COMPLETED。
        return transitionByEnterprise(scheduleId, enterpriseId,
                InterviewScheduleStatus.IN_PROGRESS, InterviewScheduleStatus.COMPLETED, null);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public InterviewScheduleCommandResultDTO markNoShow(Long scheduleId, Long enterpriseId, String reason) {
        // 候选人未到场：排期终结并自动淘汰投递。
        InterviewScheduleCommandResultDTO result = transitionByEnterprise(scheduleId, enterpriseId,
                InterviewScheduleStatus.CONFIRMED, InterviewScheduleStatus.NO_SHOW, reason);
        rejectApplicationBySchedule(scheduleId, reason);
        return result;
    }

    /**
     * 候选人侧状态推进：校验排期归属与当前状态，条件更新后返回结果。
     */
    private InterviewScheduleCommandResultDTO transitionByCandidate(Long scheduleId,
                                                                    Long candidateUserId,
                                                                    Integer expectedVersion,
                                                                    InterviewScheduleStatus from,
                                                                    InterviewScheduleStatus to,
                                                                    String reason) {
        InterviewScheduleQueryBO schedule = getSchedule(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        if (!schedule.candidateUserId().equals(candidateUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return transition(schedule, from, to, expectedVersion, reason);
    }

    /**
     * 企业侧状态推进：校验排期归属与当前状态，条件更新后返回结果。
     */
    private InterviewScheduleCommandResultDTO transitionByEnterprise(Long scheduleId,
                                                                     Long enterpriseId,
                                                                     InterviewScheduleStatus from,
                                                                     InterviewScheduleStatus to,
                                                                     String reason) {
        InterviewScheduleQueryBO schedule = getSchedule(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        if (!schedule.enterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_YOUR_ENTERPRISE);
        }
        return transition(schedule, from, to, null, reason);
    }

    /**
     * 通用排期状态推进：校验源状态与版本，执行 id + enterpriseId + 源状态 + 版本条件更新。
     */
    private InterviewScheduleCommandResultDTO transition(InterviewScheduleQueryBO schedule,
                                                         InterviewScheduleStatus from,
                                                         InterviewScheduleStatus to,
                                                         Integer expectedVersion,
                                                         String reason) {
        if (schedule.status() != from) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_STATUS_INVALID);
        }
        if (expectedVersion != null && !expectedVersion.equals(schedule.version())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }
        OffsetDateTime updatedAt = OffsetDateTime.now();
        Integer newVersion = schedule.version() + 1;
        InterviewSchedule update = InterviewSchedule.builder()
                .version(schedule.version())
                .build();
        int updated = interviewScheduleMapper.update(
                update,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InterviewSchedule>()
                        .eq(InterviewSchedule::getId, schedule.id())
                        .eq(InterviewSchedule::getEnterpriseId, schedule.enterpriseId())
                        .eq(InterviewSchedule::getStatus, from)
                        .set(InterviewSchedule::getStatus, to)
                        .set(InterviewSchedule::getStatusReason, reason)
                        .set(InterviewSchedule::getUpdatedAt, updatedAt)
                        .set(InterviewSchedule::getTraceId, TraceUtil.getTraceId()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }
        return new InterviewScheduleCommandResultDTO(
                schedule.id(), to, newVersion, updatedAt);
    }

    /**
     * 排期终结后回写投递为淘汰。
     */
    private void rejectApplicationBySchedule(Long scheduleId, String reason) {
        InterviewScheduleQueryBO schedule = getSchedule(scheduleId);
        if (schedule == null) {
            return;
        }
        jobValidationApi.markRejectedByInterview(schedule.applicationId(), schedule.enterpriseId(), reason);
    }

    private InterviewScheduleQueryBO getSchedule(Long scheduleId) {
        return interviewScheduleMapper.getScheduleWithApplicationById(scheduleId);
    }
}
