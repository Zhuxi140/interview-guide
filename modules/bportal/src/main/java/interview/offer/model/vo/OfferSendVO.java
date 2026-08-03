package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Offer 发送结果")
public record OfferSendVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "Offer状态", example = "SENT")
        OfferStatus status,

        @Schema(description = "乐观锁版本号", example = "1")
        Integer version,

        @Schema(description = "发送时间")
        OffsetDateTime sentAt
) {
}
