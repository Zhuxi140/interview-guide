package interview.candidate.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.candidate.model.entity.EnterpriseCandidate;
import interview.candidate.model.req.EnterpriseCandidatePageReq;
import interview.candidate.model.vo.EnterpriseCandidateDetailVO;
import interview.candidate.model.vo.EnterpriseCandidateListItemVO;
import interview.candidate.model.vo.EnterpriseCandidateOverviewVO;

public interface EnterpriseCandidateService extends IService<EnterpriseCandidate> {

    /**
     * 分页查询企业人才池
     * @param enterpriseId 企业 ID
     * @param req 查询参数
     * @return 人才池分页
     */
    IPage<EnterpriseCandidateListItemVO> pageCandidates(
            Long enterpriseId, EnterpriseCandidatePageReq req);

    /**
     * 查询企业人才池候选人详情
     * @param enterpriseId 企业 ID
     * @param candidateId 人才池候选人 ID
     * @return 候选人详情
     */
    EnterpriseCandidateDetailVO getCandidate(Long enterpriseId, Long candidateId);

    /**
     * 查询企业候选人综合看板
     * @param enterpriseId 企业 ID
     * @param candidateId 人才池候选人 ID
     * @return 候选人综合看板
     */
    EnterpriseCandidateOverviewVO getOverview(Long enterpriseId, Long candidateId);
}
