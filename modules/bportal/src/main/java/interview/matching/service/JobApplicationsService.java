package interview.matching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.JobApplicationStatusReq;
import interview.matching.model.req.JobApplicationSubmitReq;
import interview.matching.model.req.JobApplicationPageReq;
import interview.matching.model.req.JobApplicationWithdrawReq;
import interview.matching.model.vo.JobApplicationListItemVO;
import interview.matching.model.vo.JobApplicationSubmitVO;
import interview.matching.model.vo.JobApplicationVO;
import interview.matching.model.vo.MyApplicationListItemVO;
import interview.matching.model.vo.JobApplicationStatusVO;

/**
 * @author zhuxi
 * @apiNote 投递与初筛服务接口
 */
public interface JobApplicationsService extends IService<JobApplications> {

    /**
     * 候选人提交岗位申请
     *
     * @param jobId 岗位 ID
     * @param req   投递请求（含 resumeId）
     * @param idempotencyKey 客户端幂等键
     * @return 投递记录详情
     */
    JobApplicationSubmitVO submitApplication(
            Long jobId, JobApplicationSubmitReq req, String idempotencyKey);

    /**
     * 查询岗位投递列表
     *
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @param req          分页和筛选条件
     * @return 投递列表分页结果
     */
    IPage<JobApplicationListItemVO> pageApplications(
            Long enterpriseId, Long jobId, JobApplicationPageReq req);

    /**
     * 查询投递详情
     *
     * @param enterpriseId  企业 ID
     * @param applicationId 投递记录 ID
     * @return 投递详情（含候选人姓名、简历文件名和最新 AI 初筛建议）
     */
    JobApplicationVO getApplicationDetail(Long enterpriseId, Long applicationId);

    /**
     * HR 按期望状态推进初筛
     *
     * @param enterpriseId  企业 ID
     * @param applicationId 投递记录 ID
     * @param req           状态变更请求（REVIEWING / PASSED / REJECTED）
     * @return 状态变更结果
     */
    JobApplicationStatusVO updateApplicationStatus(
            Long enterpriseId, Long applicationId, JobApplicationStatusReq req);

    /**
     * 查询当前候选人的投递记录
     *
     * @param req 分页和筛选条件
     * @return 我的投递分页结果（含岗位标题、企业名称）
     */
    IPage<MyApplicationListItemVO> pageMyApplications(JobApplicationPageReq req);

    /**
     * 候选人撤回本人尚未进入后续流程的投递
     *
     * @param applicationId 投递 ID
     * @param req 撤回请求
     * @return 状态变更结果
     */
    JobApplicationStatusVO withdrawApplication(
            Long applicationId, JobApplicationWithdrawReq req);
}
