package interview.matching.model.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;

import java.time.OffsetDateTime;

public record JobApplicationBO(
        Long jobId,
        Long candidateId,
        Long resumeId,
        String resumeFileName,
        Integer aiMatchScore,
        JobApplicationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
