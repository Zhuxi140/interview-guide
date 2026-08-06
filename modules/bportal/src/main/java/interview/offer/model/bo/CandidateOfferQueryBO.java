package interview.offer.model.bo;

import interview.offer.model.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 候选人 Offer 与投递关联查询结果（两表互联；岗位标题由 Service 层补齐）。
 */
public record CandidateOfferQueryBO(
        Long id,
        Long enterpriseId,
        Long applicationId,
        Long jobId,
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
