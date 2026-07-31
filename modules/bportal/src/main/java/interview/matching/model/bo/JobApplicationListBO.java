package interview.matching.model.bo;

import interview.common.enums.ScreeningRecommendation;
import interview.matching.model.enums.JobApplicationStatus;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 */

public record JobApplicationListBO(
        Long id,
        Long candidateId,
        String resumeFileName,
        Integer aiScreeningScore,
        ScreeningRecommendation aiRecommendation,
        JobApplicationStatus status,
        OffsetDateTime createdAt
){}
