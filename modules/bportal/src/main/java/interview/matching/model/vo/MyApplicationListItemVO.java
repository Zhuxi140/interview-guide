package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 我的投递列表项响应（C 端投递记录专用）
 */
@Builder
public record MyApplicationListItemVO(
        Long id,
        Long jobId,
        String jobTitle,
        String enterpriseName,
        Integer aiMatchScore,
        JobApplicationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
