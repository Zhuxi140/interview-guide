package interview.offer.service;

import interview.offer.model.req.OfferCreateReq;
import interview.offer.model.req.OfferDecisionReq;
import interview.offer.model.req.OfferSendReq;
import interview.offer.model.req.OfferUpdateReq;
import interview.offer.model.req.OfferWithdrawReq;
import interview.offer.model.vo.OfferCreateVO;
import interview.offer.model.vo.OfferDecisionVO;
import interview.offer.model.vo.OfferSendVO;
import interview.offer.model.vo.OfferUpdateVO;

/**
 * Offer 写入与决策服务。
 */
public interface OfferService {

    /**
     * 创建 Offer 草稿
     * @param enterpriseId 企业ID
     * @param applicationId 投递ID
     * @param idempotencyKey 幂等键
     * @param req 创建请求
     * @return 创建结果
     */
    OfferCreateVO createOffer(Long enterpriseId,
                              Long applicationId,
                              String idempotencyKey,
                              OfferCreateReq req);

    /**
     * 修改 Offer 草稿
     * @param enterpriseId 企业ID
     * @param offerId Offer ID
     * @param req 修改请求
     * @return 修改结果
     */
    OfferUpdateVO updateOffer(Long enterpriseId, Long offerId, OfferUpdateReq req);

    /**
     * 发送 Offer
     * @param enterpriseId 企业ID
     * @param offerId Offer ID
     * @param idempotencyKey 幂等键
     * @param req 发送请求
     * @return 发送结果
     */
    OfferSendVO sendOffer(Long enterpriseId,
                          Long offerId,
                          String idempotencyKey,
                          OfferSendReq req);

    /**
     * 撤回 Offer
     * @param enterpriseId 企业ID
     * @param offerId Offer ID
     * @param req 撤回请求
     * @return 撤回结果
     */
    OfferUpdateVO withdrawOffer(Long enterpriseId, Long offerId, OfferWithdrawReq req);

    /**
     * 候选人接受或拒绝 Offer
     * @param offerId Offer ID
     * @param idempotencyKey 幂等键
     * @param req 决策请求
     * @return 决策结果
     */
    OfferDecisionVO decideOffer(Long offerId, String idempotencyKey, OfferDecisionReq req);
}
