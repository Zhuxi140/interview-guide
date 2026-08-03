package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Offer 状态或内容更新结果")
public record OfferUpdateVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "Offer状态", example = "DRAFT")
        OfferStatus status,

        @Schema(description = "乐观锁版本号", example = "1")
        Integer version,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
