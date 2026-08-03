package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "候选人 Offer 决策结果")
public record OfferDecisionVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "Offer状态", example = "ACCEPTED")
        OfferStatus status,

        @Schema(description = "决策时间")
        OffsetDateTime decidedAt
) {
}
