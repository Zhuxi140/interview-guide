package interview.matching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.req.ApplicationAiReviewReq;
import interview.matching.model.vo.ApplicationAiScreeningReviewVO;
import interview.matching.model.vo.ApplicationAiScreeningTriggerVO;
import interview.matching.model.vo.ApplicationAiScreeningVO;
import interview.resume.model.vo.CandidateProfileVO;

/**
 * HR AI 初筛服务接口。
 */
public interface ApplicationAiScreeningService
        extends IService<ApplicationAiScreening> {

    /**
     * HR 手动发起一条投递的 AI 初筛。
     *
     * @param enterpriseId 企业 ID
     * @param applicationId 投递 ID
     * @param idempotencyKey 幂等键
     * @return 任务受理结果
     */
    ApplicationAiScreeningTriggerVO accept(
            Long enterpriseId, Long applicationId, String idempotencyKey);

    /**
     * 岗位开启自动初筛时复用同一受理逻辑。
     *
     * @param application 新建投递记录
     */
    void acceptAutomatic(JobApplications application);

    /**
     * 查询一条投递的最新 AI 初筛。
     *
     * @param enterpriseId 企业 ID
     * @param applicationId 投递 ID
     * @return 最新初筛结果
     */
    ApplicationAiScreeningVO getLatest(Long enterpriseId, Long applicationId);

    /**
     * 查询一条投递实际使用的人才画像。
     *
     * @param enterpriseId 企业 ID
     * @param applicationId 投递 ID
     * @return 人才画像
     */
    CandidateProfileVO getCandidateProfile(Long enterpriseId, Long applicationId);

    /**
     * HR 确认或推翻 AI 初筛建议。
     *
     * @param enterpriseId 企业 ID
     * @param applicationId 投递 ID
     * @param screeningId 初筛任务 ID
     * @param req 审核请求
     * @return 审核结果
     */
    ApplicationAiScreeningReviewVO review(
            Long enterpriseId, Long applicationId,
            Long screeningId, ApplicationAiReviewReq req);
}
