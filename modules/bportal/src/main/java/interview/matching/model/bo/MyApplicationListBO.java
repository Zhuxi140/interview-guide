package interview.matching.model.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;

import java.time.OffsetDateTime;

public record MyApplicationListBO(
        Long id,
        Long jobId,
        Long enterpriseId,
        String jobTitle,
        Integer aiMatchScore,
        JobApplicationStatus status,
        OffsetDateTime createdAt
) {
}
