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

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 */
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
        // 从 AuthContext 获取当前 userId
        Long userId = AuthContext.getRequiredUserId();
        // 校验 enterpriseId 对应企业存在且当前用户为企业成员
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,userId);
        // JobCreateReq -> Job 实体
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

        // TODO: baseMapper.insert(job)
        baseMapper.insert(job);
        // TODO: Job -> JobCreateVO（id / title / status.getCode() / createdAt）
        return JobCreateVO.builder()
                .id(job.getId())
                .title(req.getTitle())
                .status(status != null ? status : JobStatus.OPEN)
                .createdAt(now)
                .build();
    }

    @Override
    public IPage<JobListItemVO> pageJobs(Long enterpriseId, JobListQuery query) {
        // TODO: 构建 LambdaQueryWrapper<Job>
        //   - eq Job::getEnterpriseId, enterpriseId
        //   - eq Job::getStatus, JobStatus.getJobStatus(query.getStatus())（query.status 不为空时）
        //   - keyword 不为空时拼接 or 模糊查询：title / department / location
        //   - orderByDesc Job::getCreatedAt
        JobStatus status = query.getStatus();
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .select(Job::getId, Job::getTitle,
                        Job::getDepartment,Job::getLocation,
                        Job::getStatus,Job::getCreatedAt)
                .eq(Job::getEnterpriseId,enterpriseId)
                .eq(status!=null,Job::getStatus,status)
                .and(StrUtil.isNotBlank(query.getKeyword()),
                    w -> w.like(Job::getTitle,query.getKeyword())
                            .or()
                            .like(Job::getDepartment,query.getKeyword())
                            .or()
                            .like(Job::getLocation,query.getKeyword())
                        )
                .orderByDesc(Job::getCreatedAt);
        // TODO: baseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper)
        Page<Job> jobPage = baseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        // TODO: IPage<Job> -> IPage<JobListItemVO>
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
                                //   TODO : candidateCount 暂填 0（Phase 2 接入投递数据后补充）
                                .candidateCount(0)
                                .build()
                ).toList();
        voPage.setRecords(vos);

        return voPage;
    }

    @Override
    public JobDetailVO getJobDetail(Long enterpriseId, Long jobId) {
        // TODO: baseMapper.selectById(jobId)
        Job job = lambdaQuery()
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getId, jobId)
                .one();
        // TODO: 判空 -> 抛异常（岗位不存在）
        if (job == null) {
            throw  new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        // TODO: Job -> JobDetailVO
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
    @Transactional(rollbackFor = BusinessException.class)
    public void updateJob(Long enterpriseId, Long jobId, JobUpdateReq req) {
        if (req.getMinSalary() != null && req.getMaxSalary() != null
                && req.getMinSalary().compareTo(req.getMaxSalary()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "最高薪资不能低于最低薪资");
        }
        Long userId = AuthContext.getRequiredUserId();
        int affected = baseMapper.update(null, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .set(StrUtil.isNotBlank(req.getTitle()), Job::getTitle, req.getTitle())
                .set(StrUtil.isNotBlank(req.getJdContent()), Job::getJdContent, req.getJdContent())
                .set(StrUtil.isNotBlank(req.getDepartment()), Job::getDepartment, req.getDepartment())
                .set(StrUtil.isNotBlank(req.getLocation()), Job::getLocation, req.getLocation())
                .set(req.getMinSalary() != null, Job::getMinSalary, req.getMinSalary())
                .set(req.getMaxSalary() != null, Job::getMaxSalary, req.getMaxSalary())
                .set(req.getExperienceReq() != null, Job::getExperienceReq, req.getExperienceReq())
                .set(req.getEducationReq() != null, Job::getEducationReq, req.getEducationReq())
                .set(StrUtil.isNotBlank(req.getSkillsJson()), Job::getSkillsJson, req.getSkillsJson())
                .set(Job::getUpdatedBy, userId)
                .set(Job::getTraceId, null)
                // TODO: traceId完善后，要传入
                .set(Job::getUpdatedAt, OffsetDateTime.now()));
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
                // TODO: traceId完善后，要传入
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
                // TODO: traceId完善后，要传入
                .set(Job::getUpdatedAt, OffsetDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
    }
}
