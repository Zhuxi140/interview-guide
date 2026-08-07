package interview.offer.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.offer.mapper.OfferMapper;
import interview.offer.model.entity.Offer;
import interview.offer.model.enums.OfferStatus;
import interview.offer.service.OfferExpiryTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 定期将超过决定截止时间仍为 SENT 的 Offer 推进 EXPIRED，反映候选人未在期限内回复的失效事实。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferExpiryJob {

    private static final int BATCH_SIZE = 100;

    private final OfferMapper offerMapper;
    private final OfferExpiryTxService offerExpiryTxService;

    @Scheduled(fixedDelayString = "${app.offer.expiry-delay-ms:60000}")
    public void expireDueOffers() {
        // 每轮只读取有限批次，每条候选记录使用独立短事务推进。
        OffsetDateTime now = OffsetDateTime.now();
        List<Offer> candidates = offerMapper.selectList(
                Wrappers.lambdaQuery(Offer.class)
                        .select(Offer::getId)
                        .eq(Offer::getStatus, OfferStatus.SENT)
                        .le(Offer::getExpiresAt, now)
                        .orderByAsc(Offer::getExpiresAt)
                        .last("LIMIT " + BATCH_SIZE)
        );
        for (Offer candidate : candidates) {
            try {
                offerExpiryTxService.expireDueOne(candidate.getId(), now);
            } catch (Exception e) {
                log.error("推进 Offer 过期失败。offerId={}", candidate.getId(), e);
            }
        }
    }
}