package interview.offer.model.bo;

import interview.offer.model.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 候选人 Offer 与岗位信息关联查询结果。
 */
public record CandidateOfferQueryBO(
        Long id,
        Long enterpriseId,
        Long applicationId,
        String jobTitle,
        String offerTitle,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        LocalDate plannedStartDate,
        OffsetDateTime expiresAt,
        String content,
        OfferStatus status,
        Integer version,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
