package interview.offer.model.vo;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "C端 Offer 详情")
public record CandidateOfferDetailVO(
        @Schema(description = "Offer ID", example = "36001")
        Long offerId,

        @Schema(description = "企业名称")
        String enterpriseName,

        @Schema(description = "岗位名称")
        String jobTitle,

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
        OffsetDateTime sentAt
) {
}