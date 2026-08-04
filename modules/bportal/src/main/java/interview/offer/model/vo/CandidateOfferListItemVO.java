package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "C端 Offer 列表项")
public record CandidateOfferListItemVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "企业名称")
        String enterpriseName,

        @Schema(description = "岗位名称")
        String jobTitle,

        @Schema(description = "Offer状态")
        OfferStatus status,

        @Schema(description = "过期时间")
        OffsetDateTime expiresAt,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}