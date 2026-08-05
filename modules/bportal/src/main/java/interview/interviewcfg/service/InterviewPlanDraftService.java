package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import interview.interviewcfg.model.req.InterviewPlanDraftApplyReq;
import interview.interviewcfg.model.req.InterviewPlanDraftCreateReq;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.interviewcfg.model.vo.InterviewPlanDraftApplyVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftCreateVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftDetailVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftListItemVO;

/**
 * Agent 面试编排草案服务。
 */
public interface InterviewPlanDraftService extends IService<InterviewPlanDraft> {

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

    /**
     * 分页查询投递下的编排草案
     * @param enterpriseId 企业ID
     * @param applicationId 投递ID
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @param sort 排序字段
     * @param order 排序方向
     * @return 草案分页列表
     */
    IPage<InterviewPlanDraftListItemVO> pageDrafts(Long enterpriseId,
                                                    Long applicationId,
                                                    Integer page,
                                                    Integer size,
                                                    InterviewPlanDraftStatus status,
                                                    String sort,
                                                    String order);

    /**
     * 查询草案详情
     * @param enterpriseId 企业ID
     * @param applicationId 投递ID
     * @param draftId 草案ID
     * @return 草案详情
     */
    InterviewPlanDraftDetailVO getDraftDetail(Long enterpriseId,
                                               Long applicationId,
                                               Long draftId);
}
