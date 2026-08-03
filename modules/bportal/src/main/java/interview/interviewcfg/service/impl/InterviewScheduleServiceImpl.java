package interview.interviewcfg.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.bportal.JobValidationApi;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.bo.InterviewScheduleQueryBO;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.entity.InterviewSchedule;
import interview.interviewcfg.model.req.*;
import interview.interviewcfg.model.vo.*;
import interview.interviewcfg.service.InterviewScheduleService;
import interview.interviewcfg.service.InterviewStageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewScheduleServiceImpl extends ServiceImpl<InterviewScheduleMapper, InterviewSchedule>
        implements InterviewScheduleService {

    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final InterviewScheduleMapper interviewScheduleMapper;
    private final InterviewStageTemplateService interviewStageTemplateService;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final JobValidationApi jobValidationApi;
    private final UserApi userApi;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InterviewScheduleCreateVO createSchedule(Long enterpriseId, Long applicationId, InterviewScheduleCreateReq req, String idempotencyKey) {
        // 校验企业、幂等键和投递归属。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        boolean exists = lambdaQuery()
                .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                .eq(InterviewSchedule::getUpdatedBy, AuthContext.getRequiredUserId())
                .eq(InterviewSchedule::getIdempotencyKey, idempotencyKey)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        jobValidationApi.requirePassedApplication(applicationId, enterpriseId);
        ResolvedSchedulePhase resolved = resolveSchedulePhase(
                enterpriseId, applicationId, req.templateId(), req.roundNo());

        // 校验时间并检查面试官的有效排期冲突。
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

        // 将模板快照和由轮次派生的阶段共同固化到排期。
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
                .idempotencyKey(idempotencyKey)
                .status(InterviewScheduleStatus.PENDING_CONFIRMATION)
                .version(0)
                .build();

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
        // TODO 事务提交后向候选人发送邀请通知。
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
        // 校验租户、筛选条件和白名单排序。
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

        // 分页关联投递和岗位，并批量补充候选人名称。
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
        List<Long> candidateIds = boPage.getRecords().stream()
                .map(InterviewScheduleQueryBO::candidateUserId)
                .distinct()
                .toList();
        Map<Long, String> candidateNames = candidateIds.isEmpty()
                ? Map.of()
                : userApi.getUserNamesByIds(candidateIds);
        List<InterviewScheduleListItemVO> records = boPage.getRecords().stream()
                .map(bo -> new InterviewScheduleListItemVO(
                        bo.id(),
                        bo.applicationId(),
                        bo.roundNo(),
                        bo.phaseCode(),
                        bo.phaseName(),
                        candidateNames.get(bo.candidateUserId()),
                        bo.jobTitle(),
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
        // 纯CRUD
        // 校验企业访问范围并通过企业条件读取排期详情。
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
        // 查询企业内排期并保留原状态、时间、面试官和 version；校验尚未开始且未进入终态。
        InterviewSchedule schedule = lambdaQuery()
                .select(
                        InterviewSchedule::getStatus, InterviewSchedule::getInterviewTime,
                        InterviewSchedule::getDurationMinutes,InterviewSchedule::getEnterpriseId,
                        InterviewSchedule::getInterviewerUserId, InterviewSchedule::getVersion
                )
                .eq(InterviewSchedule::getId, scheduleId)
                .one();
        if (schedule == null){
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        if (!schedule.getEnterpriseId().equals(enterpriseId)){
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_YOUR_ENTERPRISE);
        }

        boolean terminal = InterviewScheduleStatus.isTerminal(schedule.getStatus()) || schedule.getStatus().equals(InterviewScheduleStatus.IN_PROGRESS);
        if (terminal) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_ALREADY_TERMINATED);
        }
        // 校验 expectedVersion，durationMinutes 为空时保留原值。
        if (!schedule.getVersion().equals(req.expectedVersion())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_VERSION_CONFLICT);
        }

        // 新 interviewTime 必须晚于当前时间, 且需处理排期冲突
        OffsetDateTime newInterviewTime = req.interviewTime() != null ? req.interviewTime() : schedule.getInterviewTime();
        if ( req.interviewTime() != null && newInterviewTime.isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_INVALID);
        }

        Integer newDurationMinutes = req.durationMinutes();
        int finalDurationMinutes = newDurationMinutes == null ? schedule.getDurationMinutes() : newDurationMinutes;

        Long interviewerUserId = req.interviewerUserId() != null ? req.interviewerUserId() : schedule.getInterviewerUserId();
        List<InterviewSchedule> schedules = lambdaQuery()
                .select(InterviewSchedule::getInterviewTime, InterviewSchedule::getDurationMinutes)
                .eq(InterviewSchedule::getInterviewerUserId, interviewerUserId)
                .ne(InterviewSchedule::getId,scheduleId)
                .in(InterviewSchedule::getStatus,
                        InterviewScheduleStatus.PENDING_CONFIRMATION,
                        InterviewScheduleStatus.CONFIRMED,
                        InterviewScheduleStatus.IN_PROGRESS)
                .list();

        // 新面试官属于当前企业
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,interviewerUserId);


        // 使用最终 durationMinutes 计算新结束时间，检查新时间段冲突；并发场景继续依赖数据库约束兜底。
        boolean hasConflict = schedules.stream()
                .anyMatch(s -> overlaps(
                        s.getInterviewTime(),
                        s.getDurationMinutes(),
                        newInterviewTime,
                        finalDurationMinutes));

        if (hasConflict){
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_CONFLICT);
        }
        // 使用 id + enterpriseId + version + 允许源状态执行条件更新，写入新时间、时长、面试官并重置为 PENDING_CONFIRMATION。
        OffsetDateTime now = OffsetDateTime.now();
        InterviewSchedule update = InterviewSchedule.builder()
                .interviewTime(newInterviewTime)
                .durationMinutes(finalDurationMinutes)
                .interviewerUserId(interviewerUserId)
                .status(InterviewScheduleStatus.PENDING_CONFIRMATION)
                .version(schedule.getVersion() + 1)
                .updatedAt(now)
                .build();
        //  同次更新执行 version=version+1 和审计填充；
        boolean result = update(update, new LambdaUpdateWrapper<InterviewSchedule>()
                .eq(InterviewSchedule::getId, scheduleId)
                .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                .eq(InterviewSchedule::getStatus, schedule.getStatus())
                .eq(InterviewSchedule::getVersion, schedule.getVersion()));
        //更新零行时区分状态冲突与版本冲突。
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
        // TODO ⑦ 事务提交后释放原日历占用并重新通知候选人
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
        // TODO ① 查询企业内排期并校验 expectedVersion，拒绝不存在、已删除或跨企业排期。
        // TODO ② 仅允许 PENDING_CONFIRMATION/CONFIRMED 且 interviewTime 尚未到达的排期取消，CANCELLED 之后不可恢复。
        // TODO ③ 使用 id + enterpriseId + version + 源状态条件原子更新为 CANCELLED，同时递增 version 并写入审计字段。
        // TODO ④ 更新零行时重新判断重复取消、状态已变化或版本冲突，禁止无条件覆盖并发结果。
        // TODO ⑤ 在同一事务记录 fromStatus→CANCELLED 流转日志和 reason；提交后释放日历并通知相关人员。
        // TODO ⑥ 返回 id、CANCELLED、新 version 和 updatedAt。
        return null;
    }

    @Override
    @Transactional
    public InterviewScheduleUpdateVO updateStatus(Long enterpriseId, Long scheduleId, InterviewScheduleStatusReq req) {
        // TODO ① 查询企业内排期，解析目标状态并只接受 OFFERED、HIRED、REJECTED，校验 expectedVersion。
        // TODO ② 按状态机验证 COMPLETED→OFFERED/REJECTED、OFFERED→HIRED/REJECTED，禁止逆向或跨级流转。
        // TODO ③ OFFERED/HIRED 时校验 offerDetail 的 JSON 结构和必填内容；REJECTED 时按约定清理或保留录用快照。
        // TODO ④ 使用 id + enterpriseId + version + fromStatus 条件原子更新目标状态、offerDetail、version 和审计字段。
        // TODO ⑤ 更新零行时区分排期不存在、源状态已变化和乐观锁冲突。
        // TODO ⑥ 在同一事务插入 workflow_transition_logs，保存 fromStatus、toStatus、操作人和 transitionReason。
        // TODO ⑦ 事务提交后发布录用/拒绝领域事件；job_applications 继续只表示初筛事实，不反向覆盖其状态。
        // TODO ⑧ 返回最新 id、status、version 和 updatedAt。
        return null;
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
            if (roundNo != 1) {
                throw new BusinessException(ErrorCode.INTERVIEW_ROUND_SEQUENCE_INVALID);
            }
            snapshot = interviewStageTemplateService.buildTemplateSnapshot(enterpriseId, templateId);
            snapshotJson = writeTemplateSnapshot(snapshot);
        } else {
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

        // 前置轮次必须连续存在，且不能已经拒绝、取消或未到场。
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

    private record ResolvedSchedulePhase(
            String snapshotJson,
            InterviewTemplateSnapshot.StageSnapshot stage
    ) {
    }
}
