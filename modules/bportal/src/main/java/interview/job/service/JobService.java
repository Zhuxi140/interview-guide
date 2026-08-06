package interview.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.job.model.entity.Job;
import interview.job.model.req.CandidateJobSearchReq;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;

import java.util.List;
import java.util.Map;

/**
 * @author zhuxi
 */
public interface JobService extends IService<Job> {

    /**
     * 创建岗位
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
     * 查询 C 端可投递岗位
     *
     * @param req 分页与筛选参数
     * @return 可投递岗位分页结果
     */
    IPage<CandidateJobListItemVO> pageCandidateJobs(CandidateJobSearchReq req);

    /**
     * 查询 C 端可投递岗位详情
     *
     * @param jobId 岗位 ID
     * @return 可投递岗位详情
     */
    CandidateJobDetailVO getCandidateJobDetail(Long jobId);

    /**
     * 编辑岗位
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @param req          编辑参数
     * @return 岗位编辑结果
     */
    JobUpdateVO updateJob(Long enterpriseId, Long jobId, JobUpdateReq req);

    /**
     * 开关岗位（开放/关闭）
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @param req          目标状态
     * @return 岗位状态流转结果
     */
    JobStatusUpdateVO updateJobStatus(Long enterpriseId, Long jobId, JobStatusReq req);

/**
     * 删除岗位（逻辑删除）
     *
     * @param enterpriseId 企业 ID
     * @param jobId 岗位 ID
     * @param expectedVersion 客户端读取到的岗位版本
     */
    void deleteJob(Long enterpriseId, Long jobId, Integer expectedVersion);

    /**
     * 批量查询岗位标题，供关联模块按外键补齐展示字段（避免三表 JOIN）。
     *
     * @param jobIds 岗位 ID 集合
     * @return 岗位 ID → 标题 映射；jobIds 为空时返回空 Map
     */
    Map<Long, String> getJobTitlesByIds(List<Long> jobIds);
}
