package interview.matching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.JobApplicationStatusReq;
import interview.matching.model.req.JobApplicationSubmitReq;
import interview.matching.model.vo.JobApplicationListItemVO;
import interview.matching.model.vo.JobApplicationSubmitVO;
import interview.matching.model.vo.JobApplicationVO;
import interview.matching.model.vo.MyApplicationListItemVO;

/**
 * @author zhuxi
 * @apiNote 投递与初筛服务接口
 */
public interface JobApplicationsService extends IService<JobApplications> {

    /**
     * @param jobId 岗位 ID
     * @param req   投递请求（含 resumeId）
     * @return 投递记录详情
     */
    JobApplicationSubmitVO submitApplication(Long jobId, JobApplicationSubmitReq req);

    /**
     * @param enterpriseId 企业 ID
     * @param jobId        岗位 ID
     * @param page         页码
     * @param size         每页条数
     * @param status       投递状态筛选（可选）
     * @return 投递列表分页结果
     */
    IPage<JobApplicationListItemVO> pageApplications(Long enterpriseId, Long jobId, Integer page, Integer size, JobApplicationStatus status);

    /**
     * @param enterpriseId  企业 ID
     * @param applicationId 投递记录 ID
     * @return 投递详情（含候选人姓名、简历文件名、AI 匹配分）
     */
    JobApplicationVO getApplicationDetail(Long enterpriseId, Long applicationId);

    /**
     * @param enterpriseId  企业 ID
     * @param applicationId 投递记录 ID
     * @param req           状态变更请求（REVIEWING / PASSED / REJECTED）
     */
    void updateApplicationStatus(Long enterpriseId, Long applicationId, JobApplicationStatusReq req);

    /**
     * @param page   页码
     * @param size   每页条数
     * @param status 投递状态筛选（可选）
     * @return 我的投递分页结果（含岗位标题、企业名称）
     */
    IPage<MyApplicationListItemVO> pageMyApplications(Integer page, Integer size, JobApplicationStatus status);
}
