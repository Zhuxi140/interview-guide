package interview.code.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.code.model.entity.CodeSubmission;
import interview.code.model.req.EnterpriseCodeSubmissionSearchReq;
import interview.code.model.req.SessionCodeSubmissionSearchReq;
import interview.code.model.vo.CodeSubmissionDetailVO;
import interview.code.model.vo.CodeSubmissionListItemVO;
import interview.code.model.vo.EnterpriseCodeSubmissionDetailVO;
import interview.code.model.vo.EnterpriseCodeSubmissionListItemVO;

/**
 * 代码提交只读查询服务：候选人本人提交记录与企业侧候选人提交查询。
 *
 * <p>提交端点（沙箱执行 + AI 审查）为复杂流程型接口，本服务不负责创建提交。</p>
 */
public interface CodeSubmissionQueryService extends IService<CodeSubmission> {

    /**
     * 候选人分页查询本人当前会话的提交记录
     * @param sessionId 会话 ID
     * @param req 分页查询参数
     * @return 提交记录分页
     */
    IPage<CodeSubmissionListItemVO> pageMySubmissions(Long sessionId, SessionCodeSubmissionSearchReq req);

    /**
     * 候选人查询指定提交的执行与 AI 审查结果
     * @param sessionId 会话 ID
     * @param submissionId 提交记录 ID
     * @return 提交详情
     */
    CodeSubmissionDetailVO getMySubmission(Long sessionId, Long submissionId);

    /**
     * 企业侧分页查询候选人的提交记录
     * @param enterpriseId 企业 ID
     * @param candidateId 候选人用户 ID
     * @param req 分页查询参数
     * @return 提交记录分页
     */
    IPage<EnterpriseCodeSubmissionListItemVO> pageEnterpriseCandidateSubmissions(
            Long enterpriseId, Long candidateId, EnterpriseCodeSubmissionSearchReq req);

    /**
     * 企业侧查询提交详情（执行结果 + AI 审查全文 + 提交代码）
     * @param enterpriseId 企业 ID
     * @param submissionId 提交记录 ID
     * @return 提交详情
     */
    EnterpriseCodeSubmissionDetailVO getEnterpriseSubmissionDetail(Long enterpriseId, Long submissionId);
}
