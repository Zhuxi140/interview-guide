package interview.offer.service;

import interview.common.util.TraceUtil;
import interview.offer.mapper.OfferMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Offer 过期推进独立短事务服务。
 */
@Service
@RequiredArgsConstructor
public class OfferExpiryTxService {

    private final OfferMapper offerMapper;

    /**
     * 独立事务将一条超过决定截止时间的 SENT Offer 推进为 EXPIRED。
     *
     * @param offerId Offer ID
     * @param now 当前时间
     * @return 是否完成状态推进
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireDueOne(Long offerId, OffsetDateTime now) {
        return offerMapper.expireDueOffer(offerId, now, TraceUtil.getTraceId()) == 1;
    }
}