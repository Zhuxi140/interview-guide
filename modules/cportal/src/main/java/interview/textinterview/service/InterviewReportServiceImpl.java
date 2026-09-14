package interview.textinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.api.infra.FileStorageApi;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.InterviewReportMapper;
import interview.textinterview.model.entity.InterviewReport;
import interview.textinterview.model.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewReportServiceImpl
        extends ServiceImpl<InterviewReportMapper, InterviewReport>
        implements InterviewReportService {

    /** 报告 PDF 预签名下载地址有效期（秒），与简历/资质材料下载保持一致。 */
    private static final long REPORT_DOWNLOAD_TTL_SECONDS = 300L;

    private final InterviewScheduleQueryApi interviewScheduleQueryApi;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final FileStorageApi fileStorageApi;

    @Override
    public InterviewReportVO getReportBySchedule(Long enterpriseId, Long scheduleId) {
        // 纯 CRUD
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);

        // 通过排期模块校验企业归属，cportal 不直接访问排期表。
        InterviewScheduleQueryDTO schedule = interviewScheduleQueryApi.getSchedule(scheduleId);
        if (schedule == null || !enterpriseId.equals(schedule.enterpriseId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND);
        }
        return toReportVO(getReport(scheduleId, enterpriseId));
    }

    @Override
    public IPage<InterviewReportListItemVO> pageReports(Long enterpriseId, Integer page, Integer size,
                                                         InterviewReportGenerationStatus generationStatus,
                                                         String startTime, String endTime) {
        // 纯 CRUD
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        validatePage(page, size);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 报告表同时保存任务状态，PENDING 和 FAILED 记录也参与分页。
        IPage<InterviewReport> reportPage = lambdaQuery()
                .eq(InterviewReport::getEnterpriseId, enterpriseId)
                .eq(generationStatus != null,
                        InterviewReport::getGenerationStatus, generationStatus)
                .ge(start != null, InterviewReport::getCreatedAt, start)
                .le(end != null, InterviewReport::getCreatedAt, end)
                .orderByDesc(InterviewReport::getCreatedAt)
                .orderByDesc(InterviewReport::getId)
                .page(new Page<>(page, size));

        // 一次跨模块批量查询补齐展示字段，避免报告列表逐条调用。
        Map<Long, InterviewScheduleQueryDTO> schedules =
                getScheduleMap(reportPage.getRecords());
        List<InterviewReportListItemVO> records = reportPage.getRecords().stream()
                .map(report -> {
                    InterviewScheduleQueryDTO schedule = schedules.get(report.getScheduleId());
                    if (schedule != null
                            && !enterpriseId.equals(schedule.enterpriseId())) {
                        schedule = null;
                    }
                    return new InterviewReportListItemVO(
                            report.getId(),
                            report.getScheduleId(),
                            schedule == null ? null : schedule.candidateName(),
                            schedule == null ? null : schedule.jobTitle(),
                            report.getGenerationStatus(),
                            completedScore(report),
                            report.getCreatedAt()
                    );
                })
                .toList();
        return copyPage(reportPage, records);
    }

    @Override
    public IPage<InterviewReportCandidateListItemVO> pageCandidateReports(Long userId, Integer page, Integer size,
                                                                           InterviewReportGenerationStatus generationStatus) {
        // 纯 CRUD
        validateCurrentUser(userId);
        validatePage(page, size);
        // 排期归属由 bportal 内部 API 判断，报告模块只查询本人排期对应的报告。
        List<Long> scheduleIds =
                interviewScheduleQueryApi.listScheduleIdsByCandidate(userId);
        if (scheduleIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        IPage<InterviewReport> reportPage = lambdaQuery()
                .in(InterviewReport::getScheduleId, scheduleIds)
                .eq(generationStatus != null,
                        InterviewReport::getGenerationStatus, generationStatus)
                .orderByDesc(InterviewReport::getCreatedAt)
                .orderByDesc(InterviewReport::getId)
                .page(new Page<>(page, size));

        Map<Long, InterviewScheduleQueryDTO> schedules =
                getScheduleMap(reportPage.getRecords());
        List<InterviewReportCandidateListItemVO> records =
                reportPage.getRecords().stream()
                        .map(report -> {
                            InterviewScheduleQueryDTO schedule =
                                    schedules.get(report.getScheduleId());
                            return new InterviewReportCandidateListItemVO(
                                    report.getId(),
                                    report.getScheduleId(),
                                    schedule == null ? null : schedule.enterpriseName(),
                                    schedule == null ? null : schedule.jobTitle(),
                                    report.getGenerationStatus(),
                                    completedScore(report),
                                    report.getCreatedAt()
                            );
                        })
                        .toList();
        return copyPage(reportPage, records);
    }

    @Override
    public InterviewReportVO getCandidateReport(Long userId, Long scheduleId) {
        // 纯 CRUD
        validateCurrentUser(userId);

        // 候选人只能读取投递关系中归属于自己的排期报告。
        InterviewScheduleQueryDTO schedule =
                interviewScheduleQueryApi.getSchedule(scheduleId);
        if (schedule == null || !userId.equals(schedule.candidateUserId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_NOT_FOUND);
        }
        return toReportVO(getReport(scheduleId, schedule.enterpriseId()));
    }

    @Override
    public InterviewReportDownloadVO getDownloadUrl(Long userId, Long scheduleId) {
        // 复用候选人报告可见性规则：排期必须归属当前用户。
        validateCurrentUser(userId);
        InterviewScheduleQueryDTO schedule =
                interviewScheduleQueryApi.getSchedule(scheduleId);
        if (schedule == null || !userId.equals(schedule.candidateUserId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_NOT_FOUND);
        }

        // 仅 COMPLETED 且已生成 PDF 对象的报告可下载；
        // 未就绪状态用独立错误码，避免与「报告不存在」混淆。
        InterviewReport report = getReport(scheduleId, schedule.enterpriseId());
        if (report.getGenerationStatus() != InterviewReportGenerationStatus.COMPLETED
                || StrUtil.isBlank(report.getReportPdfUrl())) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_NOT_READY);
        }

        // 对内部对象键签发短期只读地址，不向客户端暴露存储定位信息。
        return InterviewReportDownloadVO.builder()
                .downloadUrl(fileStorageApi.generatePresignedDownloadUrl(
                        report.getReportPdfUrl(),
                        Duration.ofSeconds(REPORT_DOWNLOAD_TTL_SECONDS)))
                .expiresInSeconds(REPORT_DOWNLOAD_TTL_SECONDS)
                .build();
    }

    private InterviewReport getReport(Long scheduleId, Long enterpriseId) {
        InterviewReport report = lambdaQuery()
                .eq(InterviewReport::getScheduleId, scheduleId)
                .eq(InterviewReport::getEnterpriseId, enterpriseId)
                .one();
        if (report == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_NOT_FOUND);
        }
        return report;
    }

    private InterviewReportVO toReportVO(InterviewReport report) {
        InterviewReportGenerationStatus status = report.getGenerationStatus();
        InterviewReportDetailVO detail = null;
        if (status == InterviewReportGenerationStatus.COMPLETED) {
            detail = new InterviewReportDetailVO(
                    report.getId(),
                    report.getScheduleId(),
                    report.getOverallAiScore(),
                    report.getExecutiveSummary(),
                    report.getCommunicationScore(),
                    report.getCodeCapabilityReview(),
                    report.getCompletedAt()
            );
        }
        String failureReason =
                status == InterviewReportGenerationStatus.FAILED
                        ? report.getFailureReason()
                        : null;
        return new InterviewReportVO(status, failureReason, detail);
    }

    private Map<Long, InterviewScheduleQueryDTO> getScheduleMap(
            List<InterviewReport> reports) {
        List<Long> scheduleIds = reports.stream()
                .map(InterviewReport::getScheduleId)
                .distinct()
                .toList();
        return interviewScheduleQueryApi.listSchedules(scheduleIds).stream()
                .collect(Collectors.toMap(
                        InterviewScheduleQueryDTO::id,
                        Function.identity()
                ));
    }

    private Integer completedScore(InterviewReport report) {
        return report.getGenerationStatus()
                == InterviewReportGenerationStatus.COMPLETED
                ? report.getOverallAiScore()
                : null;
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

    private void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.TIME_RANGE_INVALID);
        }
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private void validateCurrentUser(Long userId) {
        if (!AuthContext.getRequiredUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }

    private <T> IPage<T> copyPage(IPage<InterviewReport> source,
                                  List<T> records) {
        Page<T> target =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(records);
        return target;
    }
}
