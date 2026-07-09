package interview.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.job.model.entity.Job;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;

public interface JobService extends IService<Job> {

    JobCreateVO createJob(Long enterpriseId, JobCreateReq req);

    IPage<JobListItemVO> pageJobs(Long enterpriseId, JobListQuery query);

    JobDetailVO getJobDetail(Long enterpriseId, Long jobId);

    JobUpdateVO updateJob(Long enterpriseId, Long jobId, JobUpdateReq req);

    JobStatusVO updateJobStatus(Long enterpriseId, Long jobId, JobStatusReq req);

    JobDeleteVO deleteJob(Long enterpriseId, Long jobId);
}
