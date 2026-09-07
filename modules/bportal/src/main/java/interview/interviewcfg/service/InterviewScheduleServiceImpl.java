package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.bportal.JobValidationApi;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.NotificationApi;
import interview.api.system.UserApi;
import interview.api.system.dto.SendNotificationCommand;
import interview.matching.model.entity.JobApplications;
import interview.common.enums.ChannelType;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.NotifyScene;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.bo.InterviewScheduleQueryBO;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.entity.InterviewSchedule;
import interview.interviewcfg.model.req.*;
import interview.interviewcfg.model.vo.*;
import interview.job.service.JobService;
import interview.matching.service.JobApplicationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewScheduleServiceImpl extends ServiceImpl<InterviewScheduleMapper, InterviewSchedule>
        implements InterviewScheduleService {

    private static final int DEFAULT_DURATION_MINUTES = 60;

    /**
     * 通知文案中的时间展示格式（按系统默认时区转换后渲染）。
     */
    private static final DateTimeFormatter NOTIFY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InterviewScheduleMapper interviewScheduleMapper;
    private final InterviewStageTemplateService interviewStageTemplateService;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final JobValidationApi jobValidationApi;
    private final UserApi userApi;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final JobApplicationsService jobApplicationsService;
    private final NotificationApi notificationApi;

    @Override
    @Transactional
    public InterviewScheduleCreateVO createSchedule(Long enterpriseId, Long applicationId, InterviewScheduleCreateReq req, String idempotencyKey) {
        // 1. 校验企业归属、幂等键、投递状态（必须为 PASSED）
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        // 面试官必须是当前企业成员（文档约束：模板和面试官必须属于当前企业）。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, req.interviewerUserId());
        boolean exists = lambdaQuery()
                .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                .eq(InterviewSchedule::getUpdatedBy, AuthContext.getRequiredUserId())
                .eq(InterviewSchedule::getIdempotencyKey, idempotencyKey)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        jobValidationApi.requireEligibleForSchedule(applicationId, enterpriseId);

        // 2. 解析轮次对应的阶段：首轮生成模板快照，后续轮次复用首轮快照并校验连续性
        ResolvedSchedulePhase resolved = resolveSchedulePhase(
                enterpriseId, applicationId, req.templateId(), req.roundNo());

        // 3. 校验面试时间合法性、面试官时间冲突（应用层预检，数据库 EXCLUDE 约束兜底）
        OffsetDateTime interviewedTime = req.interviewTime();
        if (interviewedTime.isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_INVALID);
        }
        int durationMinutes = resolveDurationMinutes(req.durationMinutes());
        Long interviewerUserId = req.interviewerUserId();
        List<InterviewSchedule> schedules = lambdaQuery()
                .select(InterviewSchedule::getInterviewTime, InterviewSchedule::getDurationMinutes)
                .eq(InterviewSchedule::getInterviewerUserId, interviewerUserId)
                .in(InterviewSchedule::getStatus,
                        InterviewScheduleStatus.PENDING_CONFIRMATION,
                        InterviewScheduleStatus.CONFIRMED,
                        InterviewScheduleStatus.IN_PROGRESS)
                .list();

        boolean hasConflict = schedules.stream()
                .anyMatch(schedule -> overlaps(
                        schedule.getInterviewTime(),
                        schedule.getDurationMinutes(),
                        interviewedTime,
                        durationMinutes));

        if (hasConflict) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_CONFLICT);
        }

        // 4. 组装排期实体：固化模板快照、派生阶段、幂等键，初始状态 PENDING_CONFIRMATION
        InterviewSchedule schedule = InterviewSchedule.builder()
                .enterpriseId(enterpriseId)
                .applicationId(applicationId)
                .templateId(req.templateId())
                .roundNo(req.roundNo())
                .phaseCode(resolved.stage().phaseCode())
                .templateSnapshotJson(resolved.snapshotJson())
                .companyUserId(AuthContext.getRequiredUserId())
                .interviewerUserId(req.interviewerUserId())
                .interviewTime(req.interviewTime())
                .durationMinutes(durationMinutes)
                .interviewType(req.interviewType())
                .idempotencyKey(idempotencyKey)
                .status(InterviewScheduleStatus.PENDING_CONFIRMATION)
                .version(0)
                .build();

        // 5. 持久化，捕获唯一约束/排斥约束冲突转为业务错误码
        try {
            save(schedule);
        } catch (DataIntegrityViolationException e) {
            String constraint = extractConstraintName(e);
            if ("uk_interview_schedule_idempotency".equals(constraint)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            } else if ("uk_interview_schedule_application_round_active".equals(constraint)
                    || "uk_interview_schedule_application_phase_active".equals(constraint)) {
                throw new BusinessException(ErrorCode.INTERVIEW_ROUND_CONFLICT);
            } else if ("ex_interview_schedule_interviewer_time".equals(constraint)) {
                throw new BusinessException(ErrorCode.INTERVIEW_TIME_CONFLICT);
            } else {
                throw e;
            }
        }
        // 6. 首轮排期创建成功后，投递进入面试阶段（PASSED → INTERVIEWING，幂等）
        if (Short.valueOf((short) 1).equals(req.roundNo())) {
            jobValidationApi.markInterviewing(applicationId, enterpriseId);
        }

        // 7. 向候选人发送面试邀请站内信：与排期落库同事务，发送异常由编排层收敛不阻塞主流程。
        notifyCandidate(schedule.getApplicationId(), enterpriseId, NotifyScene.INTERVIEW_INVITE,
                "INTERVIEW_INVITE:" + schedule.getId(),
                Map.of("interviewTime", formatNotifyTime(schedule.getInterviewTime()),
                        "durationMinutes", String.valueOf(schedule.getDurationMinutes())));
        return new InterviewScheduleCreateVO(
                schedule.getId(), schedule.getApplicationId(), schedule.getRoundNo(),
                schedule.getPhaseCode(), resolved.stage().phaseName(), schedule.getStatus(),
                schedule.getVersion(), schedule.getInterviewTime(), schedule.getDurationMinutes(),
                schedule.getCreatedAt());
    }

@Override
    public IPage<InterviewScheduleListItemVO> pageSchedules(Long enterpriseId, Integer page, Integer size,
                                                              InterviewScheduleStatus status,
                                                              String startTime, String endTime,
                                                              String sort, String order) {
        // 1. 校验租户归属、分页参数、排序白名单、时间范围
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        validateSort(sort);
        validateOrder(order);
        OffsetDateTime parsedStartTime = parseTime(startTime);
        OffsetDateTime parsedEndTime = parseTime(endTime);
        if (parsedStartTime != null
                && parsedEndTime != null
                && parsedStartTime.isAfter(parsedEndTime)) {
            throw new BusinessException(ErrorCode.TIME_RANGE_INVALID);
        }

        // 2. 分页查询：关联投递和岗位，按 interviewTime/createdAt 排序
        IPage<InterviewScheduleQueryBO> boPage =
                interviewScheduleMapper.pageSchedulesWithApplication(
                        new Page<InterviewSchedule>(page, size),
                        enterpriseId,
                        null,
                        status,
                        parsedStartTime,
                        parsedEndTime,
                        sort,
                        "asc".equalsIgnoreCase(order)
                );

        // 3. 批量补齐候选人名称与岗位标题（避免 N+1）
        List<Long> candidateIds = boPage.getRecords().stream()
                .map(InterviewScheduleQueryBO::candidateUserId)
                .distinct()
                .toList();
        Map<Long, String> candidateNames = candidateIds.isEmpty()
                ? Map.of()
                : userApi.getUserNamesByIds(candidateIds);
        List<Long> jobIds = boPage.getRecords().stream()
                .map(InterviewScheduleQueryBO::jobId)
                .distinct()
                .toList();
        Map<Long, String> jobTitles = jobService.getJobTitlesByIds(jobIds);

        // 4. 组装 VO 列表
        List<InterviewScheduleListItemVO> records = boPage.getRecords().stream()
                .map(bo -> new InterviewScheduleListItemVO(
                        bo.id(),
                        bo.applicationId(),
                        bo.roundNo(),
                        bo.phaseCode(),
                        bo.phaseName(),
                        candidateNames.get(bo.candidateUserId()),
                        jobTitles.get(bo.jobId()),
                        bo.interviewTime(),
                        bo.durationMinutes(),
                        bo.interviewType(),
                        bo.status(),
                        bo.version()
                ))
                .toList();
        Page<InterviewScheduleListItemVO> result =
                new Page<>(boPage.getCurrent(), boPage.getSize(), boPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public InterviewScheduleDetailVO getScheduleDetail(Long enterpriseId, Long scheduleId) {
        // 纯 CRUD：校验企业访问范围，按企业条件读取排期详情（含投递、岗位、候选人信息）
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId());
        InterviewScheduleQueryBO schedule =
                interviewScheduleMapper.getScheduleWithApplication(enterpriseId, scheduleId);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        return new InterviewScheduleDetailVO(
                schedule.id(),
                schedule.applicationId(),
                schedule.candidateUserId(),
                schedule.jobId(),
                schedule.templateId(),
                schedule.roundNo(),
                schedule.phaseCode(),
                schedule.phaseName(),
                schedule.interviewerUserId(),
                schedule.interviewTime(),
                schedule.durationMinutes(),
                schedule.interviewType(),
                schedule.status(),
                schedule.statusReason(),
                schedule.version(),
                schedule.createdAt(),
                schedule.updatedAt()
        );
    }

    @Override
    @Transactional
    public InterviewScheduleUpdateVO reschedule(Long enterpriseId, Long scheduleId, InterviewScheduleRescheduleReq req) {
        // 1. 查询基础字段，校验存在性、企业归属
        InterviewSchedule schedule = lambdaQuery()
                .select(
                        InterviewSchedule::getStatus, InterviewSchedule::getInterviewTime,
                        InterviewSchedule::getDurationMinutes, InterviewSchedule::getEnterpriseId,
                        InterviewSchedule::getInterviewerUserId, InterviewSchedule::getVersion,
                        InterviewSchedule::getApplicationId
                )
                .eq(InterviewSchedule::getId, scheduleId)
                .one();
        if (schedule == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        if (!schedule.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_YOUR_ENTERPRISE);
        }

        // 2. 状态机校验：仅允许非终态且非 IN_PROGRESS 的排期重新安排
        boolean terminal = InterviewScheduleStatus.isTerminal(schedule.getStatus()) || schedule.getStatus().equals(InterviewScheduleStatus.IN_PROGRESS);
        if (terminal) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_ALREADY_TERMINATED);
        }
        // 3. 乐观锁校验
        if (!schedule.getVersion().equals(req.expectedVersion())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }

        // 4. 计算新时间、时长，校验时间合法性
        OffsetDateTime newInterviewTime = req.interviewTime() != null ? req.interviewTime() : schedule.getInterviewTime();
        if (req.interviewTime() != null && newInterviewTime.isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_INVALID);
        }
        Integer newDurationMinutes = req.durationMinutes();
        int finalDurationMinutes = newDurationMinutes == null ? schedule.getDurationMinutes() : newDurationMinutes;

        // 5. 校验新面试官归属、新时间段冲突（应用层预检，数据库 EXCLUDE 约束兜底）
        Long interviewerUserId = req.interviewerUserId() != null ? req.interviewerUserId() : schedule.getInterviewerUserId();
        List<InterviewSchedule> schedules = lambdaQuery()
                .select(InterviewSchedule::getInterviewTime, InterviewSchedule::getDurationMinutes)
                .eq(InterviewSchedule::getInterviewerUserId, interviewerUserId)
                .ne(InterviewSchedule::getId, scheduleId)
                .in(InterviewSchedule::getStatus,
                        InterviewScheduleStatus.PENDING_CONFIRMATION,
                        InterviewScheduleStatus.CONFIRMED,
                        InterviewScheduleStatus.IN_PROGRESS)
                .list();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, interviewerUserId);

        boolean hasConflict = schedules.stream()
                .anyMatch(s -> overlaps(
                        s.getInterviewTime(),
                        s.getDurationMinutes(),
                        newInterviewTime,
                        finalDurationMinutes));
        if (hasConflict) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_CONFLICT);
        }

        // 6. 条件更新：id + enterpriseId + sourceStatus + version → 新时间/时长/面试官 + 状态重置为 PENDING_CONFIRMATION + version+1
        OffsetDateTime now = OffsetDateTime.now();
        InterviewSchedule update = InterviewSchedule.builder()
                .interviewTime(newInterviewTime)
                .durationMinutes(finalDurationMinutes)
                .interviewerUserId(interviewerUserId)
                .status(InterviewScheduleStatus.PENDING_CONFIRMATION)
                .version(schedule.getVersion())
                .updatedAt(now)
                .build();
        boolean result = update(update, new LambdaUpdateWrapper<InterviewSchedule>()
                .eq(InterviewSchedule::getId, scheduleId)
                .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                .eq(InterviewSchedule::getStatus, schedule.getStatus()));

        // 7. 零行更新分支：区分不存在、状态已变、版本冲突
        if (!result) {
            InterviewSchedule current = lambdaQuery()
                    .select(InterviewSchedule::getStatus)
                    .eq(InterviewSchedule::getId, scheduleId)
                    .one();
            if (current == null) {
                throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
            }
            if (!current.getStatus().equals(schedule.getStatus())) {
                throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_ALREADY_TERMINATED);
            }
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }
        // 8. 向候选人发送重新安排通知：复用邀请场景携带新时间；幂等键拼入新时间避免与首轮/上一轮互斥。
        notifyCandidate(schedule.getApplicationId(), enterpriseId, NotifyScene.INTERVIEW_INVITE,
                "INTERVIEW_INVITE:" + scheduleId + ":" + newInterviewTime.toEpochSecond(),
                Map.of("interviewTime", formatNotifyTime(newInterviewTime),
                        "durationMinutes", String.valueOf(finalDurationMinutes)));
        return InterviewScheduleUpdateVO.builder()
                .id(scheduleId)
                .status(InterviewScheduleStatus.PENDING_CONFIRMATION)
                .interviewTime(newInterviewTime)
                .durationMinutes(finalDurationMinutes)
                .version(update.getVersion())
                .updatedAt(now)
                .build();
    }

    @Override
    @Transactional
    public InterviewScheduleUpdateVO cancelSchedule(Long enterpriseId, Long scheduleId, InterviewScheduleCancelReq req) {
        // 1. 查询基础字段，校验存在性、企业归属
        InterviewSchedule schedule = lambdaQuery()
                .select(
                        InterviewSchedule::getStatus, InterviewSchedule::getInterviewTime,
                        InterviewSchedule::getDurationMinutes, InterviewSchedule::getEnterpriseId,
                        InterviewSchedule::getVersion, InterviewSchedule::getApplicationId)
                .eq(InterviewSchedule::getId, scheduleId)
                .one();
        if (schedule == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        if (!schedule.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_YOUR_ENTERPRISE);
        }
        // 2. 状态机校验：仅允许 PENDING_CONFIRMATION/CONFIRMED 且未开始的排期取消
        if (schedule.getStatus() != InterviewScheduleStatus.PENDING_CONFIRMATION
                && schedule.getStatus() != InterviewScheduleStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_ALREADY_TERMINATED);
        }
        if (schedule.getInterviewTime().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_INVALID);
        }
        // 3. 乐观锁校验
        if (!schedule.getVersion().equals(req.expectedVersion())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }

        // 4. 条件更新：id + enterpriseId + sourceStatus + version → CANCELLED + statusReason + version+1 + 审计字段
        OffsetDateTime now = OffsetDateTime.now();
        InterviewSchedule update = InterviewSchedule.builder()
                .status(InterviewScheduleStatus.CANCELLED)
                .statusReason(req.reason())
                .version(schedule.getVersion())
                .updatedBy(AuthContext.getRequiredUserId())
                .traceId(TraceUtil.getTraceId())
                .updatedAt(now)
                .build();
        boolean result = update(update, new LambdaUpdateWrapper<InterviewSchedule>()
                .eq(InterviewSchedule::getId, scheduleId)
                .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                .eq(InterviewSchedule::getStatus, schedule.getStatus()));

        // 5. 零行更新分支：已 CANCELLED 幂等返回；状态已变/版本冲突/不存在抛错
        if (!result) {
            InterviewSchedule current = lambdaQuery()
                    .select(InterviewSchedule::getStatus, InterviewSchedule::getVersion)
                    .eq(InterviewSchedule::getId, scheduleId)
                    .one();
            if (current == null) {
                throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
            }
            if (current.getStatus() == InterviewScheduleStatus.CANCELLED) {
                // 幂等：已取消，返回当前状态
                return InterviewScheduleUpdateVO.builder()
                        .id(scheduleId)
                        .status(InterviewScheduleStatus.CANCELLED)
                        .interviewTime(schedule.getInterviewTime())
                        .durationMinutes(schedule.getDurationMinutes())
                        .version(current.getVersion())
                        .updatedAt(current.getUpdatedAt())
                        .build();
            }
            if (!current.getStatus().equals(schedule.getStatus())) {
                throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_ALREADY_TERMINATED);
            }
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }

        // 6. 向候选人发送取消通知：携带原面试时间便于候选人识别是哪一场。
        notifyCandidate(schedule.getApplicationId(), enterpriseId, NotifyScene.INTERVIEW_CANCEL,
                "INTERVIEW_CANCEL:" + scheduleId,
                Map.of("interviewTime", formatNotifyTime(schedule.getInterviewTime())));
        return InterviewScheduleUpdateVO.builder()
                .id(scheduleId)
                .status(InterviewScheduleStatus.CANCELLED)
                .interviewTime(schedule.getInterviewTime())
                .durationMinutes(schedule.getDurationMinutes())
                .version(update.getVersion())
                .updatedAt(now)
                .build();
    }

    private void validateSort(String sort) {
        if (!"interviewTime".equals(sort)
                && !"createdAt".equals(sort)
                && !"updatedAt".equals(sort)) {
            throw new BusinessException(ErrorCode.SORT_FIELD_INVALID);
        }
    }

    private void validateOrder(String order) {
        if (!"asc".equalsIgnoreCase(order)
                && !"desc".equalsIgnoreCase(order)) {
            throw new BusinessException(ErrorCode.SORT_DIRECTION_INVALID);
        }
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private OffsetDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.TIME_FORMAT_INVALID);
        }
    }

    private int resolveDurationMinutes(Integer durationMinutes) {
        return durationMinutes == null ? DEFAULT_DURATION_MINUTES : durationMinutes;
    }

private ResolvedSchedulePhase resolveSchedulePhase(Long enterpriseId,
                                                        Long applicationId,
                                                        Long templateId,
                                                        Short roundNo) {
        // 查询该投递现有排期（按轮次升序），用于校验轮次连续性、模板一致性、快照复用
        List<InterviewSchedule> existing = lambdaQuery()
                .select(InterviewSchedule::getTemplateId, InterviewSchedule::getRoundNo,
                        InterviewSchedule::getPhaseCode, InterviewSchedule::getTemplateSnapshotJson,
                        InterviewSchedule::getStatus)
                .eq(InterviewSchedule::getApplicationId, applicationId)
                .orderByAsc(InterviewSchedule::getRoundNo)
                .list();
        if (existing.stream().anyMatch(schedule -> schedule.getRoundNo().equals(roundNo))) {
            throw new BusinessException(ErrorCode.INTERVIEW_ROUND_CONFLICT);
        }

        InterviewTemplateSnapshot snapshot;
        String snapshotJson;
        if (existing.isEmpty()) {
            // 首轮：必须 roundNo=1，从模板服务生成完整快照
            if (roundNo != 1) {
                throw new BusinessException(ErrorCode.INTERVIEW_ROUND_SEQUENCE_INVALID);
            }
            snapshot = interviewStageTemplateService.buildTemplateSnapshot(enterpriseId, templateId);
            snapshotJson = writeTemplateSnapshot(snapshot);
        } else {
            // 后续轮次：校验模板一致性，复用首轮快照
            boolean templateMismatch = existing.stream()
                    .anyMatch(schedule -> !templateId.equals(schedule.getTemplateId()));
            if (templateMismatch) {
                throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_MISMATCH);
            }
            snapshotJson = existing.getFirst().getTemplateSnapshotJson();
            snapshot = readTemplateSnapshot(snapshotJson);
            if (!templateId.equals(snapshot.templateId())) {
                throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_MISMATCH);
            }
        }

        // 前置轮次必须连续存在，且不能已拒绝/取消/未到场
        for (short expectedRound = 1; expectedRound < roundNo; expectedRound++) {
            short currentRound = expectedRound;
            InterviewSchedule previous = existing.stream()
                    .filter(schedule -> schedule.getRoundNo() == currentRound)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERVIEW_ROUND_SEQUENCE_INVALID));
            if (previous.getStatus() == InterviewScheduleStatus.DECLINED
                    || previous.getStatus() == InterviewScheduleStatus.CANCELLED
                    || previous.getStatus() == InterviewScheduleStatus.NO_SHOW) {
                throw new BusinessException(ErrorCode.INTERVIEW_ROUND_SEQUENCE_INVALID);
            }
        }
        InterviewTemplateSnapshot.StageSnapshot stage = snapshot.stageForRound(roundNo);
        return new ResolvedSchedulePhase(snapshotJson, stage);
    }

    private String writeTemplateSnapshot(InterviewTemplateSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            log.error("面试模板快照序列化失败。templateId={}", snapshot.templateId(), exception);
            throw new BusinessException(ErrorCode.OBJECT_TO_JSON_ERROR);
        }
    }

    private InterviewTemplateSnapshot readTemplateSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
        try {
            return objectMapper.readValue(snapshotJson, InterviewTemplateSnapshot.class);
        } catch (Exception exception) {
            log.error("面试模板快照反序列化失败", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private boolean overlaps(OffsetDateTime existingStart,
                            int existingDurationMinutes,
                            OffsetDateTime newStart,
                            int newDurationMinutes) {
        OffsetDateTime existingEnd = existingStart.plusMinutes(existingDurationMinutes);
        OffsetDateTime newEnd = newStart.plusMinutes(newDurationMinutes);
        return existingStart.isBefore(newEnd) && existingEnd.isAfter(newStart);
    }

    private String extractConstraintName(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof PSQLException psql) {
                ServerErrorMessage em = psql.getServerErrorMessage();
                return em == null ? null : em.getConstraint();  // 返回 "uk_..." 或 "ex_..."
            }
            cause = cause.getCause();
        }
        return null;
    }

    /**
     * 向投递对应的候选人发送站内信：接收人由本模块解析（job_applications.candidate_id 即候选人用户 ID），
     * 通知模块不反查投递表。投递缺失仅记录告警，通知属尽力而为不阻塞排期主流程。
     */
    private void notifyCandidate(Long applicationId, Long enterpriseId, NotifyScene scene,
                                 String idempotencyKey, Map<String, String> vars) {
        JobApplications application = jobApplicationsService.getById(applicationId);
        if (application == null || application.getCandidateId() == null) {
            log.warn("发送面试通知跳过：投递记录缺失 applicationId={} scene={}", applicationId, scene);
            return;
        }
        notificationApi.send(new SendNotificationCommand(
                enterpriseId,
                application.getCandidateId(),
                scene,
                Set.of(ChannelType.IN_APP),
                vars,
                idempotencyKey,
                AuthContext.getRequiredUserId()));
    }

    /**
     * 通知文案时间格式化：按系统默认时区展示，避免 ISO 字符串直接外发。
     */
    private String formatNotifyTime(OffsetDateTime time) {
        return time == null ? "" : time.atZoneSameInstant(ZoneId.systemDefault()).format(NOTIFY_TIME_FORMAT);
    }

    private record ResolvedSchedulePhase(
            String snapshotJson,
            InterviewTemplateSnapshot.StageSnapshot stage
    ) {
    }
}
