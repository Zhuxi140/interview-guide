package interview.job.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.job.mapper.JobMapper;
import interview.job.model.entity.Job;
import interview.job.model.enums.JobStatus;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobCreateVO createJob(Long enterpriseId, JobCreateReq req) {
        if (req.getMinSalary() != null && req.getMaxSalary() != null
                && req.getMinSalary().compareTo(req.getMaxSalary()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "最高薪资不能低于最低薪资");
        }
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        OffsetDateTime now = OffsetDateTime.now();
        JobStatus status = req.getStatus();
        Job job = Job.builder()
                .enterpriseId(enterpriseId)
                .userId(userId)
                .title(req.getTitle())
                .jdContent(req.getJdContent())
                .department(req.getDepartment())
                .location(req.getLocation())
                .status(status != null ? status : JobStatus.OPEN)
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .experienceReq(req.getExperienceReq())
                .educationReq(req.getEducationReq())
                .skillsJson(req.getSkillsJson())
                .createdAt(now)
                .build();

        baseMapper.insert(job);
        return JobCreateVO.builder()
                .id(job.getId())
                .title(req.getTitle())
                .status(status != null ? status : JobStatus.OPEN)
                .createdAt(now)
                .build();
    }

    @Override
    public IPage<JobListItemVO> pageJobs(Long enterpriseId, JobListQuery query) {
        JobStatus status = query.getStatus();
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .select(Job::getId, Job::getTitle,
                        Job::getDepartment, Job::getLocation,
                        Job::getStatus, Job::getCreatedAt)
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(status != null, Job::getStatus, status)
                .and(StrUtil.isNotBlank(query.getKeyword()),
                        w -> w.like(Job::getTitle, query.getKeyword())
                                .or()
                                .like(Job::getDepartment, query.getKeyword())
                                .or()
                                .like(Job::getLocation, query.getKeyword())
                )
                .orderByDesc(Job::getCreatedAt);
        Page<Job> jobPage = baseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<JobListItemVO> voPage = new Page<>(jobPage.getCurrent(), jobPage.getSize(), jobPage.getTotal());
        List<Job> records = jobPage.getRecords();
        List<JobListItemVO> vos = records.stream()
                .map(raw ->
                        JobListItemVO.builder()
                                .id(raw.getId())
                                .title(raw.getTitle())
                                .department(raw.getDepartment())
                                .location(raw.getLocation())
                                .status(raw.getStatus())
                                .createdAt(raw.getCreatedAt())
                                .candidateCount(0)
                                .build()
                ).toList();
        voPage.setRecords(vos);

        return voPage;
    }

    @Override
    public JobDetailVO getJobDetail(Long enterpriseId, Long jobId) {
        Job job = lambdaQuery()
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getId, jobId)
                .one();
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        return JobDetailVO.builder()
                .id(job.getId())
                .userId(job.getUserId())
                .title(job.getTitle())
                .jdContent(job.getJdContent())
                .department(job.getDepartment())
                .location(job.getLocation())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .experienceReq(job.getExperienceReq())
                .educationReq(job.getEducationReq())
                .skillsJson(job.getSkillsJson())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void updateJob(Long enterpriseId, Long jobId, JobUpdateReq req) {
        Job existing = lambdaQuery()
                .select(Job::getMinSalary, Job::getMaxSalary)
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .one();
        if (existing == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        BigDecimal mergedMin = req.getMinSalary() != null ? req.getMinSalary() : existing.getMinSalary();
        BigDecimal mergedMax = req.getMaxSalary() != null ? req.getMaxSalary() : existing.getMaxSalary();
        if (mergedMin != null && mergedMax != null && mergedMin.compareTo(mergedMax) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "最高薪资不能低于最低薪资");
        }
        Long userId = AuthContext.getRequiredUserId();
        Job update = new Job();
        update.setId(jobId);
        update.setEnterpriseId(enterpriseId);
        if (StrUtil.isNotBlank(req.getTitle())) {
            update.setTitle(req.getTitle());
        }
        if (StrUtil.isNotBlank(req.getJdContent())) {
            update.setJdContent(req.getJdContent());
        }
        if (StrUtil.isNotBlank(req.getDepartment())) {
            update.setDepartment(req.getDepartment());
        }
        if (StrUtil.isNotBlank(req.getLocation())) {
            update.setLocation(req.getLocation());
        }
        if (req.getMinSalary() != null) {
            update.setMinSalary(req.getMinSalary());
        }
        if (req.getMaxSalary() != null) {
            update.setMaxSalary(req.getMaxSalary());
        }
        if (req.getExperienceReq() != null) {
            update.setExperienceReq(req.getExperienceReq());
        }
        if (req.getEducationReq() != null) {
            update.setEducationReq(req.getEducationReq());
        }
        if (StrUtil.isNotBlank(req.getSkillsJson())) {
            update.setSkillsJson(req.getSkillsJson());
        }
        update.setUpdatedBy(userId);
        update.setUpdatedAt(OffsetDateTime.now());

        int affected = baseMapper.update(update, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void updateJobStatus(Long enterpriseId, Long jobId, JobStatusReq req) {
        Long userId = AuthContext.getRequiredUserId();
        int affected = baseMapper.update(null, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .set(Job::getStatus, req.getStatus())
                .set(Job::getUpdatedBy, userId)
                .set(Job::getTraceId, null)
                .set(Job::getUpdatedAt, OffsetDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteJob(Long enterpriseId, Long jobId) {
        Long userId = AuthContext.getRequiredUserId();
        int affected = baseMapper.update(null, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .set(Job::getIsDeleted, true)
                .set(Job::getUpdatedBy, userId)
                .set(Job::getTraceId, null)
                .set(Job::getUpdatedAt, OffsetDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
    }
}
