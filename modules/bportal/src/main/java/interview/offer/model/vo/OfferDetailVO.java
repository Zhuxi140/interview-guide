package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "B端 Offer 详情")
public record OfferDetailVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "投递ID", example = "21001")
        Long applicationId,

        @Schema(description = "Offer标题")
        String title,

        @Schema(description = "最低薪资", nullable = true)
        BigDecimal salaryMin,

        @Schema(description = "最高薪资", nullable = true)
        BigDecimal salaryMax,

        @Schema(description = "币种代码", example = "CNY", nullable = true)
        String currency,

        @Schema(description = "计划入职日期", nullable = true)
        LocalDate plannedStartDate,

        @Schema(description = "Offer过期时间")
        OffsetDateTime expiresAt,

        @Schema(description = "Offer正文")
        String content,

        @Schema(description = "Offer状态")
        OfferStatus status,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,

        @Schema(description = "发送时间", nullable = true)
        OffsetDateTime sentAt,

        @Schema(description = "决策时间", nullable = true)
        OffsetDateTime decidedAt,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}