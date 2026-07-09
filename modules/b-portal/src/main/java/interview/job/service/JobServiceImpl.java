package interview.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.exception.BusinessException;
import interview.job.mapper.JobMapper;
import interview.job.model.entity.Job;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhuxi
 */
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobCreateVO createJob(Long enterpriseId, JobCreateReq req) {
        // TODO: 从 AuthContext 获取当前 userId
        // TODO: 校验 enterpriseId 对应企业存在且当前用户为企业成员
        // TODO: JobCreateReq -> Job 实体
        //   - id: 雪花算法（ASSIGN_ID 自动生成）
        //   - enterpriseId, userId
        //   - title, jdContent, department, location
        //   - minSalary, maxSalary, experienceReq, educationReq, skillsJson
        //   - status: JobStatus.OPEN
        // TODO: baseMapper.insert(job)
        // TODO: Job -> JobCreateVO（id / title / status.getCode() / createdAt）
        return null;
    }

    @Override
    public IPage<JobListItemVO> pageJobs(Long enterpriseId, JobListQuery query) {
        // TODO: 构建 LambdaQueryWrapper<Job>
        //   - eq Job::getEnterpriseId, enterpriseId
        //   - eq Job::getStatus, JobStatus.getJobStatus(query.getStatus())（query.status 不为空时）
        //   - keyword 不为空时拼接 or 模糊查询：title / department / location
        //   - orderByDesc Job::getCreatedAt
        // TODO: baseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper)
        // TODO: IPage<Job> -> IPage<JobListItemVO>
        //   - records 逐条转换，candidateCount 暂填 0（Phase 2 接入投递数据后补充）
        return null;
    }

    @Override
    public JobDetailVO getJobDetail(Long enterpriseId, Long jobId) {
        // TODO: baseMapper.selectById(jobId)
        // TODO: 判空 -> 抛异常（岗位不存在）
        // TODO: 校验 job.getEnterpriseId().equals(enterpriseId) -> 不匹配抛异常
        // TODO: Job -> JobDetailVO（status 转 Integer）
        return null;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobUpdateVO updateJob(Long enterpriseId, Long jobId, JobUpdateReq req) {
        // TODO: baseMapper.selectById(jobId)
        // TODO: 判空 -> 抛异常
        // TODO: 校验 job.getEnterpriseId().equals(enterpriseId)
        // TODO: 仅更新非空字段（逐字段 set 或 BeanUtils.copyProperties 忽略 null）
        // TODO: baseMapper.updateById(job)（MetaObjectHandler 自动填充 updatedBy / updatedAt）
        // TODO: Job -> JobUpdateVO（id / title / status.getCode() / updatedAt）
        return null;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobStatusVO updateJobStatus(Long enterpriseId, Long jobId, JobStatusReq req) {
        // TODO: baseMapper.selectById(jobId)
        // TODO: 判空 -> 抛异常
        // TODO: 校验 job.getEnterpriseId().equals(enterpriseId)
        // TODO: job.setStatus(JobStatus.getJobStatus(req.getStatus()))
        // TODO: baseMapper.updateById(job)
        // TODO: Job -> JobStatusVO（id / title / status.getCode()）
        return null;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobDeleteVO deleteJob(Long enterpriseId, Long jobId) {
        // TODO: baseMapper.selectById(jobId)
        // TODO: 判空 -> 抛异常
        // TODO: 校验 job.getEnterpriseId().equals(enterpriseId)
        // TODO: baseMapper.deleteById(jobId)（@TableLogic 自动转 UPDATE is_deleted = true）
        // TODO: return new JobDeleteVO("删除成功")
        return null;
    }
}
