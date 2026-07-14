package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 投递记录详情响应
 */
@Builder
public record JobApplicationVO(
        Long id,
        Long enterpriseId,
        Long jobId,
        Long candidateId,
        String candidateName,
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
