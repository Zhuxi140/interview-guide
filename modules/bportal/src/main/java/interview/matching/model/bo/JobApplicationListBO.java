package interview.matching.model.bo;

import interview.matching.model.enums.JobApplicationStatus;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 */

public record JobApplicationListBO(
        Long id,
        Long candidateId,
        String resumeFileName,
        Integer aiMatchScore,
        JobApplicationStatus status,
        OffsetDateTime createdAt
){}
