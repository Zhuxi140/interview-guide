package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 投递列表项响应（HR 端投递列表专用）
 */
@Builder
public record JobApplicationListItemVO(
        Long id,
        Long candidateId,
        String candidateName,
        String resumeFileName,
        Integer aiMatchScore,
        JobApplicationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
