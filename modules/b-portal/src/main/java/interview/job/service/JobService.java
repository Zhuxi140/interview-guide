package interview.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.job.model.entity.Job;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;

/**
 * @author zhuxi
 */
public interface JobService extends IService<Job> {

    /**
     * 创建企业
     * <p>当前用户自动成为企业 OWNER</p>
     *
     * @param enterpriseId 企业 ID
     * @param req          发布岗位请求
     * @return 发布结果
     */
    JobCreateVO createJob(Long enterpriseId, JobCreateReq req);

    /**
     * 查询企业岗位列表（分页）
     *
     * @param enterpriseId 企业 ID
     * @param query        分页/筛选参数
     * @return 分页结果
     */
    IPage<JobListItemVO> pageJobs(Long enterpriseId, JobListQuery query);

    /**
     * 查询岗位详情
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @return 岗位详情
     */
    JobDetailVO getJobDetail(Long enterpriseId, Long jobId);

    /**
     * 编辑岗位
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @param req          编辑参数
     */
    void updateJob(Long enterpriseId, Long jobId, JobUpdateReq req);

    /**
     * 开关岗位（开放/关闭）
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @param req          目标状态
     */
    void updateJobStatus(Long enterpriseId, Long jobId, JobStatusReq req);

    /**
     * 删除岗位（逻辑删除）
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     */
    void deleteJob(Long enterpriseId, Long jobId);
}
