package interview.interviewcfg.service;

import interview.interviewcfg.model.req.InterviewPlanDraftApplyReq;
import interview.interviewcfg.model.req.InterviewPlanDraftCreateReq;
import interview.interviewcfg.model.vo.InterviewPlanDraftApplyVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftCreateVO;

/**
 * Agent 面试编排草案服务。
 */
public interface InterviewPlanDraftService {

    /**
     * 异步创建面试编排草案
     * @param enterpriseId 企业ID
     * @param applicationId 投递ID
     * @param idempotencyKey 幂等键
     * @param req 创建请求
     * @return 草案受理结果
     */
    InterviewPlanDraftCreateVO createDraft(Long enterpriseId,
                                            Long applicationId,
                                            String idempotencyKey,
                                            InterviewPlanDraftCreateReq req);

    /**
     * 应用面试编排草案并创建排期
     * @param enterpriseId 企业ID
     * @param applicationId 投递ID
     * @param draftId 草案ID
     * @param idempotencyKey 幂等键
     * @param req 应用请求
     * @return 草案应用结果
     */
    InterviewPlanDraftApplyVO applyDraft(Long enterpriseId,
                                          Long applicationId,
                                          Long draftId,
                                          String idempotencyKey,
                                          InterviewPlanDraftApplyReq req);
}
