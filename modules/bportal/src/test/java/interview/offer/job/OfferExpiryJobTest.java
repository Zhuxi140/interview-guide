package interview.offer.job;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.offer.mapper.OfferMapper;
import interview.offer.model.entity.Offer;
import interview.offer.model.enums.OfferStatus;
import interview.offer.service.OfferExpiryTxService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferExpiryJobTest {

    @Mock
    private OfferMapper offerMapper;
    @Mock
    private OfferExpiryTxService offerExpiryTxService;

    private OfferExpiryJob job;

    @BeforeEach
    void setUp() {
        // 注册实体元数据，使 Job 内的 lambda select 能解析列名（Spring 启动时会自动完成，单测需手动）。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "Offer"),
                Offer.class);
        job = new OfferExpiryJob(offerMapper, offerExpiryTxService);
    }

    private Offer sentOffer(Long id, OffsetDateTime expiresAt) {
        return Offer.builder()
                .id(id)
                .status(OfferStatus.SENT)
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void shouldScanAndExpireDueOffersInBatch() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Offer> candidates = List.of(
                sentOffer(36001L, now.minusMinutes(1)),
                sentOffer(36002L, now.minusHours(1)),
                sentOffer(36003L, now.minusDays(1)));
        when(offerMapper.selectList(any())).thenReturn(candidates);

        job.expireDueOffers();

        verify(offerExpiryTxService, times(3)).expireDueOne(any(), any());
        verify(offerExpiryTxService).expireDueOne(eq(36001L), any());
        verify(offerExpiryTxService).expireDueOne(eq(36002L), any());
        verify(offerExpiryTxService).expireDueOne(eq(36003L), any());
    }

    @Test
    void shouldSkipWhenNoDueOffer() {
        when(offerMapper.selectList(any())).thenReturn(List.of());

        job.expireDueOffers();

        verify(offerExpiryTxService, times(0)).expireDueOne(any(), any());
    }

    @Test
    void shouldContinueBatchWhenSingleOfferFails() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Offer> candidates = List.of(
                sentOffer(36001L, now.minusMinutes(1)),
                sentOffer(36002L, now.minusHours(1)));
        when(offerMapper.selectList(any())).thenReturn(candidates);
        doThrow(new RuntimeException("db down"))
                .when(offerExpiryTxService).expireDueOne(eq(36001L), any());

        job.expireDueOffers();

        verify(offerExpiryTxService).expireDueOne(eq(36002L), any());
    }

    @Test
    void expireDueOneShouldReturnTrueWhenUpdated() {
        OfferExpiryTxService txService = new OfferExpiryTxService(offerMapper);
        when(offerMapper.expireDueOffer(any(), any(), any())).thenReturn(1);

        assertTrue(txService.expireDueOne(36001L, OffsetDateTime.now()));
    }

    @Test
    void expireDueOneShouldReturnFalseWhenNoRowUpdated() {
        OfferExpiryTxService txService = new OfferExpiryTxService(offerMapper);
        when(offerMapper.expireDueOffer(any(), any(), any())).thenReturn(0);

        assertFalse(txService.expireDueOne(36001L, OffsetDateTime.now()));
    }
}