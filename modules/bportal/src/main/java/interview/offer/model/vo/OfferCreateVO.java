package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Offer 草稿创建结果")
public record OfferCreateVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "投递ID", example = "21001")
        Long applicationId,

        @Schema(description = "Offer状态", example = "DRAFT")
        OfferStatus status,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
