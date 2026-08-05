package interview.matching.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.WorkflowTransitionLogMapper;
import interview.interviewcfg.model.entity.WorkflowTransitionLog;
import interview.job.model.entity.Job;
import interview.job.model.enums.JobStatus;
import interview.job.service.JobService;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.bo.JobApplicationBO;
import interview.matching.model.bo.JobApplicationListBO;
import interview.matching.model.bo.MyApplicationListBO;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.*;
import interview.matching.model.vo.*;
import interview.matching.service.ApplicationAiScreeningService;
import interview.matching.service.JobApplicationsService;
import interview.resume.model.entity.Resumes;
import interview.resume.service.ResumesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author zhuxi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class JobApplicationsServiceImpl extends ServiceImpl<JobApplicationsMapper, JobApplications> implements JobApplicationsService {

    private final JobApplicationsMapper jobApplicationsMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final UserApi userApi;
    private final JobService jobService;
    private final ResumesService resumesService;
    private final ApplicationAiScreeningService applicationAiScreeningService;
    private final WorkflowTransitionLogMapper workflowTransitionLogMapper;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationSubmitVO submitApplication(
            Long jobId, JobApplicationSubmitReq req, String idempotencyKey) {
        Long userId = AuthContext.getRequiredUserId();

        // ① 校验当前用户 userType = CANDIDATE
        if (AuthContext.getUserType() != UserType.CANDIDATE) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // ② 校验岗位：存在且处于 OPEN 状态
        Job job = jobService.lambdaQuery()
                .select(Job::getEnterpriseId, Job::getStatus)
                .eq(Job::getId, jobId)
                .one();

        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        if (job.getStatus() != JobStatus.OPEN) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_CLOSED);
        }

        // 直达投递接口同样只接受正常企业的公开岗位。
        if (enterpriseValidationApi.getPublicEnterprise(job.getEnterpriseId()) == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }

        // ③ 校验简历：存在且属于当前用户
        Resumes resume = resumesService.lambdaQuery()
                .select(Resumes::getUserId)
                .eq(Resumes::getId, req.getResumeId())
                .one();
        if (resume == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESUME_IS_NOT_YOUR);
        }

        // ④ 构造投递记录，由数据库唯一索引兜底防重复
        JobApplications jobApplications = JobApplications.builder()
                .id(IdWorker.getId())
                .enterpriseId(job.getEnterpriseId())
                .jobId(jobId)
                .candidateId(userId)
                .resumeId(req.getResumeId())
                .idempotencyKey(idempotencyKey)
                .status(JobApplicationStatus.APPLIED)
                .createdAt(OffsetDateTime.now())
                .build();

        int inserted = jobApplicationsMapper.insertIgnore(jobApplications);
        if (inserted == 0) {
            // 同一幂等键重放时返回原结果；不同请求重复投递仍按业务冲突处理。
            JobApplications existing = lambdaQuery()
                    .select(JobApplications::getId, JobApplications::getEnterpriseId,
                            JobApplications::getJobId, JobApplications::getResumeId,
                            JobApplications::getStatus,
                            JobApplications::getCreatedAt)
                    .eq(JobApplications::getCandidateId, userId)
                    .eq(JobApplications::getIdempotencyKey, idempotencyKey)
                    .one();
            if (existing != null) {
                if (!jobId.equals(existing.getJobId())
                        || !req.getResumeId().equals(existing.getResumeId())) {
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
                }
                return toSubmitVO(existing);
            }
            throw new BusinessException(ErrorCode.JOB_APPLICATION_ALREADY_EXISTS);
        }

        // 岗位开启自动初筛时，在当前事务内复用同一受理服务写入任务和 Outbox。
        applicationAiScreeningService.acceptAutomatic(jobApplications);

        return toSubmitVO(jobApplications);
    }

    @Override
    public IPage<JobApplicationListItemVO> pageApplications(
            Long enterpriseId, Long jobId, JobApplicationPageReq req) {
        // 纯CRUD
        //TODO 【HR 端分页查询投递列表】
        // ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,userId);
        // ② 校验：verifyJobExists(enterpriseId, jobId)，岗位不存在抛 40001
        boolean exists = jobService.lambdaQuery()
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getId, jobId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        // ③ 构建 Page 分页对象
        Page<JobApplications> pageObj = new Page<>(req.getPage(), req.getSize());
        // ④ 调用 jobApplicationsMapper.pageApplicationsWithJoin(IPage, enterpriseId, jobId, status)
        IPage<JobApplicationListBO> boPage = jobApplicationsMapper.pageApplicationsWithJoin(
                pageObj, enterpriseId, jobId, req.getStatus(),
                "asc".equalsIgnoreCase(req.getOrder()));
        List<JobApplicationListBO> records = boPage.getRecords();
        List<Long> userIds = records.stream()
                .map(JobApplicationListBO::candidateId)
                .toList();

        Map<Long, String> nameMap = userApi.getUserNamesByIds(userIds);

        List<JobApplicationListItemVO> vos = records.stream()
                .map(bo -> new JobApplicationListItemVO(
                        bo.id(),
                        bo.candidateId(),
                        nameMap.get(bo.candidateId()),
                        bo.resumeFileName(),
                        bo.aiScreeningScore(),
                        bo.aiRecommendation(),
                        bo.status(),
                        bo.createdAt()))
                .toList();
        // ⑤ 返回 IPage<JobApplicationListItemVO>
        Page<JobApplicationListItemVO> voPage = new Page<>(boPage.getCurrent(), boPage.getSize(), boPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public JobApplicationVO getApplicationDetail(Long enterpriseId, Long applicationId) {
        // 纯CRUD
        //TODO 【查询投递详情】
        // ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,AuthContext.getRequiredUserId());

        boolean exists = lambdaQuery()
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        // ② 调用 jobApplicationsMapper.getApplicationWithJoin(applicationId, enterpriseId)
        JobApplicationBO bo = jobApplicationsMapper.getApplicationWithJoin(enterpriseId, applicationId);

        String candidateName = userApi.getUserNameById(bo.candidateId());

        // ④ 返回 JobApplicationVO
        return new JobApplicationVO(
                applicationId,
                enterpriseId,
                bo.jobId(),
                bo.candidateId(),
                candidateName,
                bo.resumeId(),
                bo.resumeFileName(),
                bo.aiScreeningScore(),
                bo.aiRecommendation(),
                bo.status(),
                bo.createdAt(),
                bo.updatedAt());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationStatusVO updateApplicationStatus(
            Long enterpriseId, Long applicationId, JobApplicationStatusReq req) {
        // HR 初筛只允许按状态机向前流转或淘汰。
        if (!isHrTransitionAllowed(req.getExpectedStatus(), req.getStatus())) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        // ②校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId());
        JobApplications locked = jobApplicationsMapper.selectByIdForUpdate(applicationId);
        if (locked == null || !enterpriseId.equals(locked.getEnterpriseId())) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }

        // 有效 AI 任务必须完成后走专用审核接口；失败任务仍允许人工兜底。
        if ((req.getStatus() == JobApplicationStatus.PASSED
                || req.getStatus() == JobApplicationStatus.REJECTED)
                && applicationAiScreeningService.lambdaQuery()
                .eq(ApplicationAiScreening::getApplicationId, applicationId)
                .in(ApplicationAiScreening::getStatus,
                        AiTaskStatus.WAITING_PROFILE,
                        AiTaskStatus.PENDING,
                        AiTaskStatus.PROCESSING,
                        AiTaskStatus.COMPLETED)
                .isNull(ApplicationAiScreening::getReviewDecision)
                .exists()) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_AI_SCREENING_STATUS_INVALID);
        }

        // 使用路径资源、租户和期望状态完成原子条件更新。
        Long userId = AuthContext.getRequiredUserId();
        OffsetDateTime updatedAt = OffsetDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getStatus, req.getExpectedStatus())
                .set(JobApplications::getStatus, req.getStatus())
                .set(JobApplications::getUpdatedBy,userId)
                .set(JobApplications::getTraceId, null)
                .set(JobApplications::getUpdatedAt, updatedAt)
                .update();
        if (!updated) {
            throwStatusConflictOrNotFound(enterpriseId, applicationId);
        }
        return new JobApplicationStatusVO(applicationId, req.getStatus(), updatedAt);
    }

    @Override
    public IPage<MyApplicationListItemVO> pageMyApplications(JobApplicationPageReq req) {
        // 纯CRUD
        //TODO 【C 端分页查询我的投递记录】
        // ① 从 AuthContext.getRequiredUserId() 获取当前用户 ID 作为 candidateId
        Long candidateId = AuthContext.getRequiredUserId();
        // ② 构建 Page 分页对象
        Page<JobApplications> rawPage = new Page<>(req.getPage(), req.getSize());
        // ③ 调用 jobApplicationsMapper.pageMyApplicationsWithJoin(IPage, candidateId, status)
        IPage<MyApplicationListBO> boPage = jobApplicationsMapper.pageMyApplicationsWithJoin(
                rawPage, candidateId, req.getStatus(),
                "asc".equalsIgnoreCase(req.getOrder()));

        List<MyApplicationListBO> records = boPage.getRecords();
        List<Long> enterpriseIds = records.stream()
                .map(MyApplicationListBO::enterpriseId)
                .toList();
        // 查询EnterpriseName
        Map<Long, String> nameMap = enterpriseValidationApi.getNameList(enterpriseIds);
        // ④ 返回 IPage<MyApplicationListItemVO>
        List<MyApplicationListItemVO> vos = records.stream()
                .map(bo -> new MyApplicationListItemVO(
                        bo.id(),
                        bo.jobId(),
                        bo.jobTitle(),
                        nameMap.get(bo.enterpriseId()),
                        bo.matchScore(),
                        bo.passProbability(),
                        bo.status(),
                        bo.createdAt()))
                .toList();

        Page<MyApplicationListItemVO> voPage = new Page<>(boPage.getCurrent(), boPage.getSize(), boPage.getTotal());
        voPage.setRecords(vos);
        return voPage;

    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationStatusVO withdrawApplication(
            Long applicationId, JobApplicationWithdrawReq req) {
        Long candidateId = AuthContext.getRequiredUserId();
        if (AuthContext.getUserType() != UserType.CANDIDATE
                || (req.getExpectedStatus() != JobApplicationStatus.APPLIED
                && req.getExpectedStatus() != JobApplicationStatus.REVIEWING)) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        // 撤回只允许当前候选人从 APPLIED/REVIEWING 原子推进到 WITHDRAWN。
        OffsetDateTime updatedAt = OffsetDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getCandidateId, candidateId)
                .eq(JobApplications::getStatus, req.getExpectedStatus())
                .set(JobApplications::getStatus, JobApplicationStatus.WITHDRAWN)
                .set(JobApplications::getUpdatedBy, candidateId)
                .set(JobApplications::getTraceId, null)
                .set(JobApplications::getUpdatedAt, updatedAt)
                .update();
        if (!updated) {
            boolean exists = lambdaQuery()
                    .eq(JobApplications::getId, applicationId)
                    .eq(JobApplications::getCandidateId, candidateId)
                    .exists();
            throw new BusinessException(exists
                    ? ErrorCode.JOB_APPLICATION_STATUS_INVALID
                    : ErrorCode.APPLICATION_NOT_FOUND);
        }
        return new JobApplicationStatusVO(
                applicationId, JobApplicationStatus.WITHDRAWN, updatedAt);
    }

    private JobApplicationSubmitVO toSubmitVO(JobApplications application) {
        return JobApplicationSubmitVO.builder()
                .id(application.getId())
                .enterpriseId(application.getEnterpriseId())
                .jobId(application.getJobId())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationStatus markInterviewing(
            Long enterpriseId, Long applicationId, Long operatorUserId) {
        return transitionByAllowedSources(enterpriseId, applicationId,
                JobApplicationStatus.INTERVIEWING, operatorUserId, null,
                JobApplicationStatus.PASSED);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationStatus markRejectedByInterview(
            Long enterpriseId, Long applicationId, Long operatorUserId, String reason) {
        return transitionByAllowedSources(enterpriseId, applicationId,
                JobApplicationStatus.REJECTED, operatorUserId, reason,
                JobApplicationStatus.PASSED, JobApplicationStatus.INTERVIEWING);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationStatus markOffered(
            Long enterpriseId, Long applicationId, Long operatorUserId) {
        return transitionByAllowedSources(enterpriseId, applicationId,
                JobApplicationStatus.OFFERED, operatorUserId, null,
                JobApplicationStatus.INTERVIEWING);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationStatus markHired(
            Long enterpriseId, Long applicationId, Long operatorUserId) {
        return transitionByAllowedSources(enterpriseId, applicationId,
                JobApplicationStatus.HIRED, operatorUserId, null,
                JobApplicationStatus.OFFERED);
    }

    private JobApplicationStatus transitionByAllowedSources(
            Long enterpriseId, Long applicationId, JobApplicationStatus target,
            Long operatorUserId, String reason,
            JobApplicationStatus... allowedSources) {
        // 按 id + enterpriseId 读取当前状态，校验存在性与租户隔离
        JobApplications current = lambdaQuery()
                .select(JobApplications::getStatus)
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .one();
        if (current == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        JobApplicationStatus currentStatus = current.getStatus();
        if (currentStatus == target) {
            // 幂等：已处于目标状态直接返回
            return currentStatus;
        }
        boolean allowed = java.util.Arrays.asList(allowedSources).contains(currentStatus);
        if (!allowed) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        // 条件原子更新：id + enterpriseId + 源状态 → 目标状态，防止并发覆盖
        OffsetDateTime updatedAt = OffsetDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .eq(JobApplications::getStatus, currentStatus)
                .set(JobApplications::getStatus, target)
                .set(JobApplications::getUpdatedBy, operatorUserId)
                .set(JobApplications::getTraceId, TraceUtil.getTraceId())
                .set(JobApplications::getUpdatedAt, updatedAt)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        // 记录招聘状态流转日志，系统自动推进时 operatorUserId 为 0
        WorkflowTransitionLog transitionLog = WorkflowTransitionLog.builder()
                .enterpriseId(enterpriseId)
                .applicationId(applicationId)
                .fromStatus(currentStatus)
                .toStatus(target)
                .operatorUserId(operatorUserId)
                .transitionReason(reason)
                .traceId(TraceUtil.getTraceId())
                .build();
        workflowTransitionLogMapper.insert(transitionLog);
        return target;
    }

    private boolean isHrTransitionAllowed(
            JobApplicationStatus expected, JobApplicationStatus target) {
        return (expected == JobApplicationStatus.APPLIED
                && (target == JobApplicationStatus.REVIEWING
                || target == JobApplicationStatus.REJECTED))
                || (expected == JobApplicationStatus.REVIEWING
                && (target == JobApplicationStatus.PASSED
                || target == JobApplicationStatus.REJECTED));
    }

    private void throwStatusConflictOrNotFound(Long enterpriseId, Long applicationId) {
        boolean exists = lambdaQuery()
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .exists();
        throw new BusinessException(exists
                ? ErrorCode.JOB_APPLICATION_STATUS_INVALID
                : ErrorCode.APPLICATION_NOT_FOUND);
    }

}
